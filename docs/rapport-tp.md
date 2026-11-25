# TP1 - Processus unifié et approches agiles

**Projet : ChargeSlot**  
**Membres : à compléter avec vos noms réels**  
**Livraison locale : 25 novembre 2026**  
**État : réalisation locale ; publication GitHub et revue externe à effectuer par l'équipe.**

## 1. Vision en cinq lignes

Les conducteurs de véhicules électriques rencontrent des files d'attente et une disponibilité imprévisible des bornes de recharge.  
ChargeSlot permet de consulter et de réserver à l'avance un connecteur compatible avec leur véhicule.  
Le système empêche les doubles réservations et détecte les conflits liés à la maintenance et à la puissance électrique disponible.  
Les opérateurs administrent les stations, leurs connecteurs et leurs périodes d'indisponibilité.  
La plateforme réduit l'attente, améliore l'utilisation des bornes et rend la recharge plus fiable.

## 2. Acteurs et périmètre

| Acteur | Responsabilités |
|---|---|
| Conducteur | Consulter les disponibilités de ses véhicules, réserver, consulter et annuler ses réservations, lire ses notifications. |
| Opérateur de station | Ajouter/modifier/désactiver stations et connecteurs, planifier/supprimer les maintenances et intervenir sur les réservations. |
| Fournisseur de notifications | Livrer les confirmations et annulations ; représenté ici par un adaptateur local remplaçable. |

L'administrateur facultatif du document initial est fusionné avec l'opérateur. Le système lui-même n'est pas un acteur externe. UC3 est une sous-fonction incluse dans UC2 ; son isolation permet de suivre le risque technique demandé par le sujet.

Hors périmètre : facturation/paiement, commande physique de recharge, inscription libre, application mobile et e-mail/SMS réels.

## 3. Les six cas d'utilisation

| ID | Cas d'utilisation | Acteur initiateur | Résultat |
|---|---|---|---|
| UC1 | Consulter les disponibilités | Conducteur / opérateur | Connecteurs compatibles disponibles sur tout le créneau et à la puissance demandée. |
| UC2 | Réserver un créneau de recharge | Conducteur / opérateur | Réservation persistée avec statut CONFIRMED, ou refus explicite. |
| UC3 | Vérifier les conflits | Inclus dans UC2 | Détection des chevauchements, incompatibilités, maintenances et dépassements de puissance. |
| UC4 | Annuler une réservation | Propriétaire / opérateur | Statut CANCELLED et créneau libéré. |
| UC5 | Notifier le conducteur | Déclenché après UC2/UC4 | Notification locale SENT ou FAILED, sans suppression de la réservation. |
| UC6 | Gérer l'infrastructure | Opérateur | Stations/connecteurs administrés et maintenances maîtrisées. |

Relations : UC2 inclut UC3 et UC5 ; UC4 inclut UC5. Les [fiches UC](use-cases.md) détaillent préconditions, scénario nominal, alternatives et postconditions.

## 4. Priorisation

Échelle : 1 faible, 5 très élevé. Somme simple pour rendre le choix explicite ; en cas d'égalité, le risque guide la décision.

| Ordre | UC | Valeur métier | Risque | Impact architecture | Total |
|---|---|---:|---:|---:|---:|
| 1 | UC3 - Vérifier les conflits | 5 | 5 | 5 | 15 |
| 2 | UC2 - Réserver | 5 | 4 | 5 | 14 |
| 3 | UC1 - Disponibilités | 4 | 2 | 3 | 9 |
| 4 | UC5 - Notifications | 3 | 3 | 3 | 9 |
| 5 | UC6 - Infrastructure | 3 | 2 | 3 | 8 |
| 6 | UC4 - Annulation | 3 | 1 | 2 | 6 |

**Premier incrément : UC2 + UC3.** Il valide à la fois la valeur principale et le risque. Une simple liste de stations ne permettrait pas de prouver la maîtrise des conflits.

## 5. Trois itérations, quatre phases UP

Il s'agit du plan de développement et des critères de validation, pas d'un historique Git inventé. La réalisation locale a été produite dans cette session ; l'équipe doit effectuer elle-même la traçabilité Git/GitHub selon le guide.

| Itération | Phase(s) | Contenu et preuve attendue | Jalon |
|---|---|---|---|
| I1 | Inception | Vision, acteurs, six UC, priorisation, périmètre et risques. | LCO : objectifs compris et acceptés par l'équipe. |
| I1 | Élaboration | Route POST complète contrôleur/service/JPA/H2, vérification des conflits, premier test automatisé et CI distante verte. | LCA : architecture exécutable ; tag `v0.1-elaboration`. |
| I2 | Construction | Finalisation des six UC, administration, notifications, tests unitaires et intégration. | IOC : fonctions principales utilisables ; tag proposé `v0.2-construction`. |
| I3 | Fin de Construction + Transition | Interface locale, démonstration, tests de concurrence, documentation, revue par une autre équipe et corrections. | Product Release : version présentable acceptée ; tag proposé `v1.0.0`. |

Pour Inception, un tag `v0.0-inception` est proposé afin que les quatre phases aient chacune un tag de jalon. Le sujet impose le nom `v0.1-elaboration` ; les autres noms sont des conventions du projet.

Milestones à créer manuellement : `1-inception-lco`, `2-elaboration-lca`, `3-construction-ioc`, `4-transition-release`. Ne fermer LCA qu'après le passage réel de la CI distante ; ne déclarer la revue achevée qu'après retour d'une autre équipe.

## 6. Risque technique et architecture exécutable

La route centrale est `POST /api/reservations`. Elle traverse `ReservationController`, `ReservationService`, `ConflictEngine`, les repositories et H2. Elle retourne `201 Created` et enregistre une réservation, ou `409 Conflict` sans nouvel enregistrement.

Le patron Strategy sépare quatre politiques : `OverlapPolicy`, `CompatibilityPolicy`, `MaintenancePolicy`, `PowerCapacityPolicy`. Le service transactionnel prend un verrou pessimiste sur la station avant lecture des réservations puis insertion. Deux requêtes concurrentes ne peuvent ainsi confirmer un même connecteur ou dépasser ensemble la capacité électrique.

La puissance requise doit être compatible avec le véhicule, le connecteur et le minimum de puissance station disponible sur **tout** le créneau. Un balayage chronologique calcule le pic réellement simultané. Deux réservations consécutives de 50 kW ne consomment pas 100 kW simultanément.

Les notifications sont déclenchées après commit dans une transaction distincte. L'échec de l'adaptateur est enregistré comme FAILED ; la réservation reste valide. Le fonctionnement local est une simulation explicite d'un fournisseur de notifications.

Voir [l'architecture détaillée](architecture.md) et les [diagrammes rendus](diagrams/index.html).

## 7. Traçabilité des UC

| UC | Ticket à créer | Branche à utiliser | Principaux éléments locaux | Vérification |
|---|---|---|---|---|
| UC1 | UC1 - Consulter les disponibilités | `feature/uc01-disponibilites` | AvailabilityController, CatalogService, ConflictEngine | Filtrage station/type, exclusion des conflits. |
| UC2 | UC2 - Réserver un créneau | `feature/uc02-reservation` | ReservationController, ReservationService, Reservation, repositories | 201, persistance et statut CONFIRMED. |
| UC3 | UC3 - Vérifier les conflits | `feature/uc03-conflits` | ConflictEngine et quatre stratégies | Tests unitaires des limites + conflit HTTP 409 + concurrence. |
| UC4 | UC4 - Annuler une réservation | `feature/uc04-annulation` | CancellationController, ReservationService.cancel | Propriétaire/opérateur, statut CANCELLED, réutilisation du créneau. |
| UC5 | UC5 - Notifications locales | `feature/uc05-notifications` | NotificationService, NotificationGateway, NotificationRepository | SENT, FAILED et réservation conservée. |
| UC6 | UC6 - Gérer l'infrastructure | `feature/uc06-infrastructure` | InfrastructureController, InfrastructureService | Droits opérateur, maintenance, désactivation et capacité. |

Le code préparé localement ne constitue pas une preuve de ticket, branche ou PR déjà créé. Le [guide GitHub](GITHUB-PAS-A-PAS.md) donne une procédure d'import progressive et honnête des fichiers.

## 8. Validation et Definition of Done

Les tests sont dérivés des scénarios UC : nominal valide, alternatives métier, refus d'accès et absence d'effet secondaire en cas d'échec. Les résultats locaux réellement mesurés figurent dans [VALIDATION.md](VALIDATION.md).

Un UC est terminé pour la remise lorsque ses critères sont satisfaits, ses tests passent, son ticket et sa branche existent, la CI distante est verte, les diagrammes correspondent au code et la revue a été effectuée. La chaîne demandée est **UC → ticket → branche → classes → tests**.

Le code et les tests locaux sont disponibles. L'exécution GitHub, les tags/milestones, l'identification réelle des membres et la revue par une autre équipe restent à effectuer par les personnes concernées.

## 9. Vérification des consignes du sujet

| Consigne | Livrable / état |
|---|---|
| Petit système de 4 à 6 UC dont un à risque | Six UC ; UC3 et réservation concurrente. |
| Vision en cinq lignes et acteurs | Sections 1 et 2. |
| Liste et priorisation sur trois critères | Sections 3 et 4. |
| Trois itérations, quatre phases et jalons | Section 5. |
| Dépôt, tableau, tickets, milestones, branches et tags | Procédure préparée ; à exécuter manuellement sur Git/GitHub. |
| Diagramme de cas d'utilisation en PlantUML | `diagrams/use-cases.puml`, SVG et PNG ; commit à faire. |
| Architecture de bout en bout et premier test en CI | Code + ElaborationIntegrationTest ; local vérifié, CI distante à lancer soi-même. |
| Tag v0.1-elaboration et fermeture du milestone | À faire après preuve de CI distante verte. |
| Classes et séquence dérivées d'une fiche UC | `classes.puml`, `classes-architecture.puml`, `reservation-sequence.puml`. |
| Implémentation en couches | Packages controller, dto, service, domain, repository, exception. |
| Tests unitaires et intégration du nominal | ConflictPoliciesTest, ElaborationIntegrationTest et ReservationIntegrationTest. |
| Revue croisée par une autre équipe | Grille prête ; vraie revue à réaliser. |

## 10. Sources et décisions

Le sujet du TP et le document de transmission d'origine sont conservés dans `references/`. Les consignes du sujet servent de critères ; le document de transmission sert de proposition fonctionnelle. Les choix Java/Spring/H2, interface locale, comptes de démonstration et notifications simulées relèvent de la réalisation.

Évolutions par rapport au README initial : `vehicleId` référence un véhicule persisté au lieu de croire les limites envoyées par le client ; authentification et CSRF ; verrou de station pour la concurrence ; calcul exact du pic électrique ; fiches des six UC ; revue externe distinguée de la relecture entre membres.
