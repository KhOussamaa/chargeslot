# UC3 - Vérifier les conflits (risque technique)

## Objectif

Implémenter et vérifier le cas d'utilisation décrit dans docs/use-cases.md.

## Traçabilité

- Branche : `feature/uc03-conflits`.
- Milestone : `2-elaboration-lca`.
- Classes : ConflictEngine et quatre ConflictPolicy.
- Tests : ConflictPoliciesTest et les deux tests parallelRequests... de ReservationIntegrationTest.

## Critères d'acceptation

- [ ] Détecter tous les chevauchements et traiter l'adjacence.
- [ ] Contrôler compatibilité, maintenance et limites matérielles.
- [ ] Calculer le pic réellement simultané de puissance station.
- [ ] Retourner 409 avec motifs et puissance disponible si pertinente.
- [ ] Empêcher doublon et surcapacité lors de requêtes concurrentes.

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

