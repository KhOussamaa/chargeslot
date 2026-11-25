# UC6 - Gérer l'infrastructure

## Objectif

Implémenter et vérifier le cas d'utilisation décrit dans docs/use-cases.md.

## Traçabilité

- Branche : `feature/uc06-infrastructure`.
- Milestone : `3-construction-ioc`.
- Classes : InfrastructureController, InfrastructureService.
- Tests : operatorCanCreateStationAndConnector, maintenanceCannotInvalidateConfirmedReservation, stationPowerCannotBeReducedBelowConfirmedLoad.

## Critères d'acceptation

- [ ] Réserver les opérations à l'opérateur.
- [ ] Créer/modifier/désactiver stations et connecteurs.
- [ ] Créer/lister/supprimer les maintenances.
- [ ] Refuser une maintenance qui invalide une réservation confirmée.
- [ ] Refuser une baisse de capacité sous les réservations confirmées.

## Definition of Done

- [ ] Tests locaux vérifiés et CI distante verte.
- [ ] PR liée à cette issue, avec revue réelle.
- [ ] Documentation et diagrammes cohérents.
- [ ] Preuves et liens ajoutés avant clôture.

## Preuves à compléter

PR : ...
Run CI : ...
Relecteur réel : ...
Remarques : ...

