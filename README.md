# ChargeSlot - TP1 Processus unifié et approches agiles

Application locale de réservation de bornes de recharge. Les six cas d'utilisation du document de transmission sont implémentés : disponibilités, réservation, conflits, annulation, notifications locales et infrastructure.

## Démarrer en deux minutes

Prérequis : **JDK 21**. Une connexion Internet est nécessaire pour la première compilation ; Maven est fourni sous forme de wrapper. Aucun serveur de base de données à installer.

Ouvrir PowerShell **dans le dossier `chargeslot`** extrait de l'archive :

```powershell
java -version
java -jar .\chargeslot.jar
```

Ouvrir ensuite **http://127.0.0.1:8080**. Arrêter avec `Ctrl+C` dans ce terminal.

Le JAR fourni est déjà compilé. Pour reconstruire les sources et exécuter les tests :

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
java -jar .\target\chargeslot-1.0.0.jar
```

Sous Linux/macOS : `chmod +x mvnw`, puis `./mvnw clean verify` et `java -jar target/chargeslot-1.0.0.jar`.

Alternative PowerShell : `powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1`. L'option Bypass s'applique uniquement au processus lancé ; elle ne modifie pas la politique globale de Windows.

## Comptes pédagogiques

| Compte | Mot de passe de démonstration | Rôle | Conducteur |
|---|---|---|---|
| `alice` | `AliceDemo21!` | Conductrice | 17 |
| `bob` | `BobDemo21!` | Conducteur | 18 |
| `operateur` | `OperateurDemo21!` | Opérateur | Peut gérer les réservations des conducteurs |

Ces comptes sont prédéfinis pour le TP. L'application écoute uniquement sur `127.0.0.1`. L'authentification HTTP Basic et la protection CSRF sont actives. Les droits sont vérifiés côté serveur.

Au premier démarrage, les données comprennent deux stations, cinq connecteurs et trois véhicules. La station Campus possède **80 kW partagés** : deux connecteurs libres ne garantissent pas que 50 + 40 kW soient disponibles simultanément.

## Documents à lire

- [Rapport du TP](docs/rapport-tp.md) : vision, périmètre, priorités, phases, jalons et couverture des consignes.
- [Fiches des six UC](docs/use-cases.md) : scénarios, alternatives et critères d'acceptation.
- [Architecture et règles métier](docs/architecture.md).
- [Diagrammes](docs/diagrams/index.html) : sources PlantUML, SVG et PNG déjà générés.
- [Démonstration guidée](docs/DEMONSTRATION.md).
- [Guide Git et GitHub à faire soi-même](docs/GITHUB-PAS-A-PAS.md).
- [Grille de revue par une autre équipe](docs/REVUE-CROISEE.md).
- [Résultats de validation locale](docs/VALIDATION.md).

## Ce qui est livré et ce qui reste humain

Le code, les tests, l'interface, les diagrammes et la documentation sont fournis localement. Le workflow `.github/workflows/ci.yml` est préparé **sans publication ni exécution sur GitHub**. Aucun dépôt, ticket, branche, tag ou milestone n'a été créé à ta place.

Il reste à suivre le guide GitHub, constater la CI distante verte, puis faire réaliser et signer la revue par une **autre équipe**. La grille livrée n'est pas une revue déjà effectuée. Les validations pédagogiques LCO/LCA/IOC/Release ne sont pas présumées acquises.

## Architecture

```text
Interface locale / client HTTP
          -> contrôleurs REST et DTO validés
          -> services transactionnels
          -> moteur de conflits (Strategy)
          -> repositories Spring Data JPA
          -> H2
```

Java 21, Spring Boot 3.5.16, Spring Web, Data JPA, Validation, Security, H2, Maven Wrapper 3.9.11, JUnit 5 et MockMvc. Les politiques de conflit sont indépendantes : chevauchement, compatibilité/limites matérielles, maintenance et puissance globale.

Les données H2 sont conservées dans `data/` relativement au dossier où l'application est lancée. Les tests utilisent des bases mémoire séparées ; ils n'effacent pas les données de démonstration.

Pour une démonstration vierge qui disparaît à l'arrêt :

```powershell
java -jar .\chargeslot.jar '--spring.datasource.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1'
```

Pour changer de port : `java -jar .\chargeslot.jar --server.port=8081`.

## API principale

Toutes les routes ci-dessous nécessitent l'authentification, sauf `/api/csrf`. Les écritures nécessitent aussi le cookie de session et le jeton obtenu par `GET /api/csrf`.

| UC | Méthode et route | Accès |
|---|---|---|
| Session | `GET /api/me`, `GET /api/csrf` | Compte courant / jeton |
| Catalogue | `GET /api/stations`, `/api/stations/{id}`, `/api/connectors/{id}`, `/api/vehicles` | Authentifié ; véhicules du propriétaire |
| UC1 | `GET /api/availability?vehicleId=...&startAt=...&endAt=...&requestedPowerKw=...` | Propriétaire ou opérateur ; filtres `stationId`, `connectorType` facultatifs |
| UC2/UC3 | `POST /api/reservations` | Propriétaire ou opérateur |
| UC2 | `GET /api/reservations`, `GET /api/reservations/{id}` | Réservations du propriétaire, ou toutes pour l'opérateur |
| UC4 | `POST /api/reservations/{id}/cancel` | Propriétaire ou opérateur |
| UC5 | `GET /api/notifications` | Notifications du propriétaire, ou toutes pour l'opérateur |
| UC6 | `POST /api/stations`, `PATCH /api/stations/{id}` | Opérateur |
| UC6 | `POST /api/stations/{id}/connectors`, `PATCH /api/connectors/{id}` | Opérateur |
| UC6 | `GET /api/maintenances`, `GET /api/maintenances/{id}`, `POST /api/maintenances`, `DELETE /api/maintenances/{id}` | Opérateur |

Les dates de l'API sont au format ISO 8601 avec fuseau, par exemple `2026-11-26T14:00:00Z` (utiliser une date future au moment de la démonstration). L'interface convertit automatiquement les heures du navigateur en UTC.

Corps de réservation : `driverId`, `vehicleId`, `connectorId`, `startAt`, `endAt`, `requestedPowerKw`. Le véhicule et sa puissance maximale viennent de la base ; les limites ne sont pas déclarées librement par le client comme dans l'exemple initial.

Réponses : `201` réservation créée, `400` données invalides, `401` authentification requise, `403` droit/CSRF, `404` ressource absente, `409` conflit métier. Une puissance insuffisante renvoie `availablePowerKw` et une proposition ; elle n'est jamais réduite silencieusement.

## Tests et démonstration automatisée

```powershell
.\mvnw.cmd verify
# Dans un deuxième terminal, application déjà démarrée :
powershell -ExecutionPolicy Bypass -File .\scripts\demo-api.ps1
```

Les tests couvrent le scénario nominal de bout en bout, les conflits, les annulations, les droits, l'échec des notifications et les accès concurrents. Voir `docs/VALIDATION.md` pour le résultat réellement obtenu.

## Limites assumées du TP

- Notifications **locales** : journal et liste consultable, sans envoi réel d'e-mail/SMS.
- Comptes et véhicules prédéfinis : pas de création de compte ou d'inscription de véhicule dans ce périmètre.
- H2 et comptes de démonstration adaptés à une présentation locale ; pas d'intégration à une borne physique ni de paiement.
- Les événements après commit évitent qu'une panne de livraison annule la réservation ; pas de garantie de livraison après un arrêt brutal entre commit et traitement (une outbox serait une extension).
- Marge opérationnelle facultative : `--chargeslot.operational-margin-minutes=10` ; défaut 0, maximum 60 minutes.
