# Démonstration ChargeSlot (environ 7 minutes)

## Préparer

Depuis le dossier chargeslot, démarrer une base mémoire vierge :

```powershell
java -jar .\chargeslot.jar '--spring.datasource.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1'
```

Ouvrir http://127.0.0.1:8080. Choisir un début demain à 14 h et une fin à 15 h dans le navigateur. Conserver les mêmes heures pour tous les essais simultanés.

## 1. Présenter le problème (30 secondes)

« ChargeSlot vérifie le temps, la compatibilité, la maintenance et la puissance partagée. Deux connecteurs libres ne suffisent donc pas à garantir deux recharges simultanées. »

Montrer les six UC dans le rapport et signaler le risque UC3.

## 2. Réserver normalement (1 minute)

Se connecter avec Alice. Choisir sa voiture CCS, station Campus, demain 14 h-15 h, 50 kW. Rechercher puis réserver Campus CCS A. Montrer la réservation confirmée et la notification locale.

## 3. Démontrer le risque (1 minute)

Se déconnecter puis se connecter avec Bob. Même période, station Campus, 40 kW : la recherche ne propose aucun connecteur CCS compatible avec cette puissance, car seuls 30 kW restent disponibles.

Réduire à 30 kW : Campus CCS B devient disponible. Le réserver. Expliquer `50 + 30 = 80 kW`. Le chevauchement sur A est aussi exclu.

Pour montrer explicitement le HTTP 409 et la proposition chiffrée, utiliser le script API sur une base vierge, ou les tests. L'interface masque normalement les connecteurs indisponibles plutôt que de proposer un bouton voué au refus.

## 4. Annuler et libérer (45 secondes)

Revenir à Alice ; annuler sa réservation avant son début. Montrer le statut Annulée et la notification. Rechercher à nouveau : le connecteur et sa puissance sont libérés.

## 5. Gestion opérateur (1 minute)

Se connecter avec operateur. Ajouter une station et un connecteur. Planifier une maintenance sur un créneau sans réservation. Une maintenance de station touche tous ses connecteurs ; une maintenance de connecteur ne touche que lui.

Essayer une maintenance Campus qui recoupe une réservation encore confirmée de Bob : le système refuse et demande d'annuler d'abord. Montrer qu'un conducteur ne voit pas les formulaires opérateur et que les tests contrôlent aussi l'accès côté serveur.

## 6. Montrer les preuves techniques (1 minute)

Ouvrir le diagramme de classes du domaine, celui des classes applicatives et le diagramme de séquence. Suivre une réservation : contrôleur → service → verrou de station → politiques → repository → commit → notification.

Lancer ou montrer le résultat :

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Mettre en avant ElaborationIntegrationTest (201 puis 409 sans doublon), les deux tests de concurrence et le test de panne de notification. Les résultats locaux sont dans VALIDATION.md. La CI distante doit être montrée depuis le compte GitHub après exécution par l'équipe.

## 7. Terminer avec l'état réel (30 secondes)

Montrer la traçabilité UC → ticket → branche → classes → tests après création GitHub. Montrer la grille de revue remplie par une autre équipe lorsqu'elle sera réalisée. Ne pas présenter la grille vierge comme une revue terminée.

## Script HTTP reproductible

Dans un deuxième terminal, avec l'application démarrée sur une base mémoire vierge :

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-api.ps1
```

Le script découvre les identifiants, récupère les jetons CSRF, crée 50 kW pour Alice, constate le refus de 40 kW pour Bob, accepte 30 kW et annule les réservations créées par le script dans son bloc de nettoyage. Ne pas l'exécuter en même temps que la démonstration manuelle sur le même créneau.

## Questions à préparer pour l'oral

- Pourquoi UC3 avant une interface complète ? Pour réduire d'abord le risque portant sur la validité des réservations.
- Pourquoi un verrou sur la station ? La puissance est partagée entre connecteurs ; verrouiller seulement un connecteur ne suffit pas.
- Pourquoi calculer un pic ? Additionner toutes les réservations qui croisent un grand intervalle surestime parfois la consommation simultanée.
- Pourquoi Strategy ? Chaque règle est indépendante et testable ; on peut ajouter une politique.
- Pourquoi les notifications après commit ? Ne pas notifier une réservation annulée par rollback ; une panne de transport ne doit pas annuler la réservation.
- Quelles limites ? Comptes et véhicules prédéfinis, notification locale, H2 local, pas de borne physique ni de paiement.
