# UC4 - Annuler une réservation

## Objectif

Implémenter et vérifier le cas d'utilisation décrit dans docs/use-cases.md.

## Traçabilité

- Branche : `feature/uc04-annulation`.
- Milestone : `3-construction-ioc`.
- Classes : CancellationController, ReservationService.cancel.
- Tests : cancellationReleasesSlotAndIsIdempotent, anotherDriverCannotCancel, operatorCanCancel.

## Critères d'acceptation

- [ ] Autoriser uniquement le propriétaire ou l'opérateur.
- [ ] Conserver la réservation au statut CANCELLED.
- [ ] Libérer le créneau et la puissance.
- [ ] Rendre l'annulation idempotente sans notification doublée.

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

