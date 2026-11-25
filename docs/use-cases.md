# Fiches des cas d'utilisation ChargeSlot

Les identifiants UC1 à UC6 correspondent au document de transmission. Les dates utilisent des intervalles `[début, fin[` : une réservation finissant à 15 h et une commençant à 15 h sont adjacentes, sans conflit si la marge est nulle.

## UC1 - Consulter les disponibilités

**Acteur :** conducteur, ou opérateur pour un conducteur.  
**But :** trouver un connecteur réellement utilisable pour un véhicule et une puissance.  
**Préconditions :** authentification et véhicule existant appartenant au conducteur, sauf opérateur.  
**Déclencheur :** saisie d'un intervalle et d'une puissance, avec station/type facultatifs.

Scénario nominal :
1. Le conducteur choisit un de ses véhicules, un début, une fin et une puissance.
2. Il peut restreindre la station ou le type de connecteur.
3. Le système valide l'intervalle et les droits sur le véhicule.
4. Il vérifie chaque connecteur candidat avec les mêmes politiques que la réservation.
5. Il affiche uniquement les connecteurs utilisables et leur puissance maximale disponible sur tout le créneau.

Alternatives : données invalides → 400 ; véhicule/station absent → 404 ; véhicule d'un tiers → 403 ; aucune disponibilité → liste vide 200. Une disponibilité peut changer avant confirmation : UC2 revérifie toujours sous verrou.

**Postcondition :** aucune donnée métier modifiée.  
**Acceptation :** filtre station/type effectif, réservations et maintenances exclues, puissance globale prise en compte.  
**Route :** GET `/api/availability`.

## UC2 - Réserver un créneau de recharge

**Acteur :** conducteur, ou opérateur pour son compte.  
**But :** confirmer un connecteur compatible à une puissance déterminée.  
**Préconditions :** compte authentifié, jeton CSRF valide, véhicule/station/connecteur existants.  
**Déclencheur :** confirmation d'une demande.

Scénario nominal :
1. Le conducteur consulte UC1 et choisit un connecteur.
2. Il confirme son véhicule, l'intervalle et la puissance.
3. Le système valide le JSON et les dates ; le début doit être futur, au plus à 366 jours, pour une durée maximale de 24 h.
4. Il vérifie que le conducteur et le propriétaire du véhicule correspondent et que l'acteur a le droit d'agir.
5. Il prend le verrou de la station.
6. Il charge les réservations/maintenances et exécute UC3.
7. Il persiste une réservation CONFIRMED, à la puissance exactement demandée.
8. Il publie un événement et valide la transaction.
9. UC5 traite la confirmation après commit.
10. Le client reçoit 201 Created, un identifiant et l'en-tête Location.

Alternatives : JSON/dates/puissance invalides → 400 ; ressource absente → 404 ; identité interdite → 403 ; conflit UC3 → 409, avec motifs. Une puissance station insuffisante contient `availablePowerKw` et une proposition. Le conducteur doit renvoyer une nouvelle demande s'il accepte la réduction. Un échec de notification ne supprime pas la réservation.

**Postconditions :** succès → une réservation CONFIRMED ; refus → aucune nouvelle réservation, aucune notification de confirmation.  
**Acceptation :** persistance de bout en bout, 201, absence de doublon, données vérifiées côté serveur.  
**Routes :** POST `/api/reservations` ; GET de la liste et de `/{id}` pour le suivi.

## UC3 - Vérifier les conflits (risque technique)

**Initiateur :** UC2 ; la même logique est réutilisée en lecture par UC1.  
**But :** empêcher une recharge impossible.  
**Préconditions :** demande validée, données du véhicule/connecteur/station chargées ; verrou détenu pendant une confirmation.  
**Déclencheur :** appel de ConflictEngine.

Scénario nominal :
1. Vérifier le chevauchement sur le même connecteur, marge facultative comprise.
2. Vérifier l'activation, le type de connecteur et les limites véhicule/connecteur.
3. Vérifier la maintenance du connecteur ou de la station.
4. Calculer le pic de puissance des autres réservations pendant l'intervalle.
5. Vérifier `pic existant + puissance demandée <= capacité station`.
6. Retourner une liste de conflits vide et autoriser UC2 à poursuivre.

Alternatives : OVERLAP, INACTIVE, INCOMPATIBLE, VEHICLE_POWER_LIMIT, CONNECTOR_POWER_LIMIT, MAINTENANCE ou POWER_ADJUSTMENT_REQUIRED. Plusieurs motifs peuvent être retournés ; le code principal correspond au premier selon l'ordre des politiques.

**Postcondition :** aucune écriture ; UC2 décide de persister seulement si la liste est vide.  
**Acceptation :** cas inclus/englobant/partiel, adjacence, maintenance, limites, pic réel et concurrence testés.  
**Route :** fonction interne incluse dans POST `/api/reservations`, pas d'acteur « système » fictif.

## UC4 - Annuler une réservation

**Acteur :** propriétaire ou opérateur.  
**But :** libérer le créneau avant son début.  
**Préconditions :** authentification, CSRF et réservation existante.  
**Déclencheur :** action « Annuler ».

Scénario nominal :
1. L'acteur sélectionne une réservation.
2. Le système prend le verrou de sa station et contrôle les droits.
3. Il vérifie que la réservation n'a pas commencé.
4. Il passe le statut à CANCELLED, sans supprimer l'historique.
5. Il valide la transaction et déclenche UC5.
6. Il retourne 200 avec le statut annulé ; les prochaines recherches ignorent cette réservation.

Alternatives : réservation absente → 404 ; autre conducteur → 403 ; déjà commencée → 409 ALREADY_STARTED ; déjà annulée → 200 sans deuxième notification.

**Postcondition :** le créneau et la puissance sont libérés pour les prochaines réservations.  
**Acceptation :** propriétaire/opérateur uniquement, annulation idempotente, nouvelle réservation possible.  
**Route :** POST `/api/reservations/{id}/cancel`.

## UC5 - Notifier le conducteur

**Acteur bénéficiaire :** conducteur ; fournisseur de notification en support.  
**But :** tracer la confirmation ou l'annulation.  
**Précondition :** transaction UC2/UC4 validée.  
**Déclencheur :** événement ReservationEvent après commit.

Scénario nominal :
1. NotificationService reçoit l'événement dans une transaction distincte.
2. Il construit le message avec l'identifiant de réservation et le type d'événement.
3. L'adaptateur local écrit le message dans le journal.
4. Une Notification SENT est persistée.
5. Le conducteur retrouve la notification dans son espace ; l'opérateur voit la liste globale.

Alternative : exception de l'adaptateur → Notification FAILED persistée ; la réservation déjà confirmée/annulée reste dans son état. Aucun e-mail ou SMS réel n'est envoyé dans ce TP.

**Postcondition :** résultat de la tentative consultable.  
**Acceptation :** confirmation et annulation visibles, séparation entre utilisateurs, panne simulée testée.  
**Route de consultation :** GET `/api/notifications`.

## UC6 - Gérer les stations, connecteurs et maintenances

**Acteur :** opérateur.  
**But :** maintenir une infrastructure cohérente.  
**Préconditions :** rôle opérateur authentifié et CSRF valide pour les écritures.  
**Déclencheur :** formulaire de gestion.

Scénario nominal :
1. L'opérateur crée ou modifie une station (nom, capacité, activation).
2. Il crée ou modifie ses connecteurs (nom, type, puissance, activation).
3. Pour une maintenance, il choisit une station, éventuellement un connecteur, un intervalle et un motif.
4. Le système prend le verrou de la station et vérifie les réservations existantes.
5. Il persiste la modification et retourne la ressource ; une maintenance peut ensuite être supprimée.

Alternatives : conducteur ordinaire → 403 ; données invalides → 400 ; ressource absente → 404 ; connecteur d'une autre station → 400 ; maintenance bloquant une réservation confirmée → 409 RESERVATION_EXISTS ; capacité réduite sous une charge déjà confirmée → 409 CAPACITY_IN_USE ; type/puissance de connecteur invalidant une réservation → 409 CONNECTOR_IN_USE.

Désactiver une station ou un connecteur interdit les **nouvelles** réservations ; les réservations existantes restent visibles et doivent être annulées explicitement si nécessaire. Il n'y a pas de suppression silencieuse de réservation.

**Postcondition :** catalogue mis à jour sans rendre impossible une réservation déjà confirmée par une réduction de capacité/limite ou l'ajout d'une maintenance.  
**Acceptation :** gestion réservée à l'opérateur, contrôle des références, opérations reflétées dans UC1/UC2.  
**Routes :** POST/PATCH stations et connecteurs, GET/POST/DELETE maintenances ; voir README pour les chemins complets.
