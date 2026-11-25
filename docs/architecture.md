# Architecture et décisions de conception

## Couches

`controller` expose les routes REST. `dto.ApiModels` décrit les demandes validées et les réponses. `service` applique les droits et les transactions. `service.conflict` porte les règles métier. `repository` utilise Spring Data JPA. `domain` contient les entités. `exception` transforme les erreurs en réponses HTTP explicites.

Les contrôleurs ne retournent pas directement les entités JPA. Les DTO sont construits dans les services, à l'intérieur des transactions, avec `open-in-view=false`. Aucune dépendance à l'interface n'existe dans le domaine.

## Domaine et relations

- Une ChargingStation possède plusieurs Connector et une puissance électrique totale.
- Un Vehicle appartient à un conducteur identifié par `driverId` et possède un type de connecteur et une limite de puissance persistés.
- Une Reservation relie un Vehicle à un Connector, pour un intervalle et une puissance fixes.
- Une MaintenanceWindow vise toute une station ou un connecteur de cette station.
- Une Notification conserve le résultat de la livraison locale et l'identifiant de la réservation.

Les identités des conducteurs proviennent des trois comptes pédagogiques, sans entité d'inscription utilisateur. Le véhicule persiste le rattachement au conducteur. Les types sont CCS, TYPE2 et CHADEMO ; l'usage d'adaptateurs physiques n'est pas modélisé.

## Intervalles et limites

Tous les instants sont des `Instant` UTC. Les heures locales sont converties dans l'interface. Les intervalles sont semi-ouverts `[début, fin[` et se chevauchent si `nouveauDébut < ancienneFin && nouvelleFin > ancienDébut`.

Le début doit être futur et au plus à 366 jours ; la durée maximale est 24 h. Ces limites bornent la démonstration et sont des décisions du projet. La puissance est un BigDecimal positif, avec au plus sept chiffres entiers et deux décimales. Aucune arithmétique binaire flottante n'est utilisée dans les règles Java.

La marge facultative s'applique au chevauchement des réservations sur un même connecteur. Elle ne représente pas une consommation d'électricité et ne prolonge pas le calcul de puissance. Valeur par défaut 0 ; exemple `--chargeslot.operational-margin-minutes=10`.

## Patron Strategy

ConflictEngine reçoit les beans implémentant ConflictPolicy, dans l'ordre :

1. OverlapPolicy : connecteur réservé, marge comprise ; les CANCELLED sont ignorées.
2. CompatibilityPolicy : station/connecteur actifs, type compatible et limites véhicule/connecteur.
3. MaintenancePolicy : maintenance de station ou du connecteur recoupant le créneau.
4. PowerCapacityPolicy : capacité station suffisante à chaque instant du créneau.

Chaque politique retourne des Conflict ; le moteur collecte tous les motifs. En confirmation, une liste non vide devient ApiException HTTP 409. Le même moteur est utilisé par UC1, afin d'éviter des règles divergentes entre recherche et réservation.

## Calcul de la capacité partagée

Pour chaque réservation existante confirmée, on clippe son intervalle au créneau demandé. On ajoute `+puissance` au début et `-puissance` à la fin dans une TreeMap. Les variations d'un même instant sont additionnées avant le calcul du pic. La puissance station disponible est `capacité - pic des réservations existantes`.

Exemple Campus 80 kW : 50 kW de 14 h à 15 h et 50 kW de 15 h à 16 h donnent un pic de 50 kW, pas 100. Une nouvelle demande à 30 kW de 14 h à 16 h est acceptable sur un autre connecteur. Une demande à 40 kW sur un intervalle où 50 sont déjà utilisés est refusée avec une proposition de 30 kW.

La demande doit également respecter `puissance <= limite connecteur` et `puissance <= limite véhicule`. Le système refuse ou propose explicitement ; il ne réduit pas automatiquement une puissance acceptée. Une réservation confirmée conserve la puissance demandée pour tout son intervalle.

Complexité du pic : O(n log n) pour n réservations candidates ; adaptée au petit système du TP. Une stratégie plus avancée pourrait gérer des profils de puissance variables.

## Concurrence et transactions

La simple suite « chercher un conflit, puis enregistrer » est vulnérable à deux requêtes simultanées. ReservationService prend donc un verrou `PESSIMISTIC_WRITE` sur la ligne ChargingStation avant de charger les données et de confirmer. Toutes les opérations d'écriture affectant une station existante suivent ce même verrou : réservation, annulation, modification station/connecteur et maintenance.

La recherche initiale de l'identifiant de station ne charge pas prématurément le connecteur dans le contexte JPA. Les données métier sont chargées après acquisition du verrou. Sous l'isolation READ_COMMITTED utilisée par H2, la seconde transaction voit la réservation que la première vient de valider.

Le verrou couvre toute la station et protège aussi deux connecteurs distincts partageant sa puissance. Les tests d'intégration lancent deux transactions dans deux threads et attendent exactement une création et un conflit, pour le doublon et pour la puissance.

UC1 est une lecture indicative, sans garantie de réservation ; UC2 revérifie sous verrou. Le choix d'un verrou par station simplifie la preuve de correction au prix d'une sérialisation des écritures de cette station.

## Notifications après commit

ReservationService publie un ReservationEvent dans sa transaction. NotificationService utilise `@TransactionalEventListener(AFTER_COMMIT)` et une transaction `REQUIRES_NEW`. Il ne traite donc qu'une réservation effectivement validée.

NotificationGateway sépare la livraison du métier ; LocalNotificationGateway écrit dans les logs. Une exception de cet adaptateur est capturée et une notification FAILED est enregistrée. Les tests injectent une panne et vérifient que la réservation reste CONFIRMED.

Il n'y a pas de transport e-mail/SMS ni de reprise automatique. Un crash entre commit et livraison peut perdre l'événement ; une outbox persistante constituerait une amélioration de production. Une panne de la base des notifications après commit ne peut pas annuler le commit de réservation.

## Sécurité de la démonstration

Les mots de passe des comptes pédagogiques sont encodés avec BCrypt à l'exécution. L'identité et le rôle sont issus de Spring Security, jamais d'un champ `role` envoyé par le navigateur. Un conducteur ne peut utiliser le véhicule d'un autre, lister ses réservations/notifications ou annuler à sa place. L'opérateur peut intervenir pour les conducteurs.

Les écritures exigent un jeton CSRF et le cookie de session associé. L'interface obtient le jeton via `/api/csrf` puis le renvoie dans son en-tête. Le script PowerShell utilise également une session. Les credentials de l'interface restent en mémoire de la page et sont effacés à sa déconnexion ; pas de stockage local permanent.

L'écoute est limitée à la boucle locale. Ces comptes prédéfinis et HTTP local servent au TP ; un déploiement public demanderait notamment HTTPS, gestion des comptes et secrets, et un autre paramétrage de base.

## Sources techniques consultées

- [Spring Boot 3.5 - prérequis](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- [Spring Data JPA - verrouillage](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
- [Spring Framework - événements transactionnels](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).
- [PlantUML - ligne de commande](https://plantuml.com/command-line).

Ces références expliquent les mécanismes employés ; la preuve locale de fonctionnement est fournie par les tests exécutés et décrits dans VALIDATION.md.
