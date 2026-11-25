# UC2 - Réserver un créneau

## Objectif

Implémenter et vérifier le cas d'utilisation décrit dans docs/use-cases.md.

## Traçabilité

- Branche : `feature/uc02-reservation`.
- Milestone : `2-elaboration-lca`.
- Classes : ReservationController, ReservationService, ReservationRepository.
- Tests : ElaborationIntegrationTest et nominalScenarioPersistsAndNotifiesAfterCommit.

## Critères d'acceptation

- [ ] Retourner 201 Created pour une demande valide.
- [ ] Persister une réservation CONFIRMED.
- [ ] Valider les dates, la puissance et le propriétaire du véhicule.
- [ ] Ne pas enregistrer de réservation en cas de conflit.

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

