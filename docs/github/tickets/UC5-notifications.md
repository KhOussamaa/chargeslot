# UC5 - Notifications locales

## Objectif

Implémenter et vérifier le cas d'utilisation décrit dans docs/use-cases.md.

## Traçabilité

- Branche : `feature/uc05-notifications`.
- Milestone : `3-construction-ioc`.
- Classes : NotificationService, NotificationGateway, LocalNotificationGateway, NotificationRepository.
- Tests : notificationFailureDoesNotRollBackReservation et nominalScenarioPersistsAndNotifiesAfterCommit.

## Critères d'acceptation

- [ ] Traiter confirmation et annulation après commit.
- [ ] Tracer SENT ou FAILED.
- [ ] Conserver la réservation si le transport échoue.
- [ ] Ne montrer au conducteur que ses notifications.
- [ ] Annoncer clairement la simulation locale sans e-mail/SMS réel.

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

