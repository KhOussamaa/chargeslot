# Git et GitHub : le faire toi-même

Ce guide est à exécuter par toi. Aucun dépôt Git/GitHub, compte, ticket, tableau, branche, PR, tag ou milestone n'a été créé automatiquement. Le workflow YAML est simplement prêt dans les fichiers.

Le code est déjà réalisé localement. Les étapes ci-dessous constituent un **import progressif de cette livraison**, suivi de vérifications et revues réelles. Elles ne doivent pas être présentées comme un historique de développement antérieur. Ne change pas les dates des commits et n'invente pas d'auteur ou de revue.

## 0. Choisir le bon cas

**Si tes camarades ont déjà un dépôt :** demande son URL et leur branche de travail. Clone-le dans un nouveau dossier, conserve leurs fichiers, puis importe les changements de cette livraison dans une branche dédiée après comparaison. Ne réinitialise pas leur dépôt et ne remplace pas leur historique. Les sections tickets/tableau/CI/jalons restent applicables ; les commandes d'initialisation ci-dessous sont réservées au cas d'un nouveau dépôt.

**Si aucun dépôt n'existe :** extrais l'archive et ouvre PowerShell dans le dossier `chargeslot`. Les instructions suivantes supposent ce cas et un dépôt distant vide.

Vérifie :

```powershell
git --version
java -version
Get-Location
Test-Path .git
```

La dernière commande doit retourner False pour la procédure de nouveau dépôt. Le JDK doit être 21. Complète aussi tes vrais noms dans le rapport.

## 1. Créer le dépôt vide sur GitHub

1. Connecte-toi à ton compte GitHub.
2. Clique sur **+ → New repository**.
3. Nom : `chargeslot`. Description : « TP1 - Processus unifié et approches agiles ».
4. Choisis la visibilité convenue avec ton équipe/enseignante.
5. **N'ajoute pas** de README, `.gitignore` ou licence depuis l'assistant GitHub : ces fichiers existent localement et le dépôt doit être vide pour la suite.
6. Clique sur **Create repository**, puis copie l'URL HTTPS du dépôt.
7. Dans **Settings → Collaborators**, invite tes vrais camarades si nécessaire. Cette invitation est à faire toi-même.

## 2. Initialiser Git et déposer le cadrage (Inception)

Configurer l'identité **du dépôt local** avec tes coordonnées réelles :

```powershell
git init -b main
git config user.name "TON VRAI NOM"
git config user.email "TON ADRESSE GIT OU NOREPLY GITHUB"
git add .gitignore .gitattributes pom.xml mvnw mvnw.cmd .mvn .github README.md docs scripts
git add src/main/java/com/chargeslot/ChargeSlotApplication.java
git add src/main/resources/application.properties
git update-index --chmod=+x mvnw
git diff --cached --stat
git commit -m "docs: importer le cadrage UP et le squelette Maven"
git remote add origin https://github.com/TON-COMPTE/chargeslot.git
git push -u origin main
```

Remplace les placeholders avant d'exécuter. L'authentification Git peut ouvrir le navigateur via Git Credential Manager. Ne colle pas ton mot de passe GitHub dans un fichier ou une commande ; les comptes applicatifs Alice/Bob n'ont aucun rapport avec ton compte GitHub.

Cette première étape importe le cadrage et le squelette ; les sources métier non encore ajoutées restent sur le disque en fichiers non suivis. **N'utilise pas `git add .` à ce stade**, sinon tout le code partirait directement sur main et tu perdrais l'import par UC expliqué ci-dessous.

Une commande Maven exécutée localement voit aussi les sources non suivies présentes sur le disque. Pour les jalons intermédiaires, la CI distante vérifie donc l'état réellement committé et fait foi.

## 3. Créer les quatre milestones

Dans le dépôt : **Issues → Milestones → New milestone**. Crée :

| Milestone | Description à saisir | Condition de clôture |
|---|---|---|
| `1-inception-lco` | Vision, périmètre, acteurs, UC et priorités validés. | Équipe d'accord sur les objectifs ; documents présents. |
| `2-elaboration-lca` | Architecture exécutable de réservation et maîtrise des conflits. | UC2/UC3 committés, test nominal/conflit et CI distante verte, tag v0.1-elaboration. |
| `3-construction-ioc` | Six UC utilisables et tests de leurs critères. | Branches fonctionnelles fusionnées, tests d'intégration complets verts. |
| `4-transition-release` | Interface, démonstration, revue externe et corrections. | Documentation à jour, vraie revue d'une autre équipe, corrections acceptées. |

Tu peux ajouter un ticket de cadrage à Inception et un ticket de revue/démonstration à Transition. Les six UC constituent les tickets fonctionnels principaux ; ces tickets d'organisation sont supplémentaires.

## 4. Créer les tickets UC

Dans **Issues → New issue**, copie le titre et le contenu de chaque fichier dans `docs/github/tickets/`. Conserve un tableau des **numéros réellement attribués** : UC1 n'est pas forcément l'issue #1.

| UC | Fichier de contenu | Milestone |
|---|---|---|
| UC1 | `UC1-disponibilites.md` | 3-construction-ioc |
| UC2 | `UC2-reservation.md` | 2-elaboration-lca |
| UC3 | `UC3-conflits.md` | 2-elaboration-lca |
| UC4 | `UC4-annulation.md` | 3-construction-ioc |
| UC5 | `UC5-notifications.md` | 3-construction-ioc |
| UC6 | `UC6-infrastructure.md` | 3-construction-ioc |

Assigne les tickets à leurs responsables réels. Labels proposés : `type:use-case`, `priority:P0` pour UC2/UC3, `priority:P1` pour UC1/UC5, `priority:P2` pour UC4/UC6, `phase:elaboration` ou `phase:construction`, `risk:technical` pour UC3. Les labels sont des conventions utiles du projet.

## 5. Créer le tableau de bord

1. Depuis ton profil GitHub : **Projects → New project → Board**.
2. Nom : `ChargeSlot - suivi UP`.
3. Configure le champ Status avec **Backlog → Ready → In Progress → Review → Done**.
4. Ajoute les six issues du dépôt au projet et lie le projet au dépôt depuis son onglet Projects si nécessaire.
5. Place UC3 et UC2 en Ready ; les autres en Backlog.
6. Déplace un ticket en In Progress lors de son import/vérification, puis Review lors de sa PR. Done signifie que les critères et vérifications sont réellement satisfaits.

## 6. Importer UC3 dans sa branche

Cette étape ajoute le domaine et les éléments partagés nécessaires au moteur de conflits. Certaines classes communes portent déjà les structures des UC suivants ; leur préparation technique est normale, leurs routes dédiées seront importées séparément.

```powershell
git switch -c feature/uc03-conflits
git add src/main/java/com/chargeslot/domain
git add src/main/java/com/chargeslot/repository
git add src/main/java/com/chargeslot/dto
git add src/main/java/com/chargeslot/exception
git add src/main/java/com/chargeslot/config
git add src/main/java/com/chargeslot/service/conflict
git add src/main/java/com/chargeslot/service/Actor.java
git add src/main/java/com/chargeslot/service/Intervals.java
git add src/main/java/com/chargeslot/service/CatalogService.java
git add src/main/java/com/chargeslot/controller/SessionController.java
git add src/main/java/com/chargeslot/controller/CatalogController.java
git add src/test/java/com/chargeslot/ConflictPoliciesTest.java
git diff --cached --stat
git commit -m "feat(UC3): importer le moteur de conflits et ses tests unitaires"
git push -u origin feature/uc03-conflits
```

Créer la PR sur GitHub : **Pull requests → New pull request**, base `main`, compare `feature/uc03-conflits`. Dans la description, écrire le problème, ce que vérifient les quatre politiques, les tests, et `Refs #NUMERO_REEL_UC3` en remplaçant le numéro.

Attendre la CI verte, demander une vraie relecture à un camarade et fusionner. Ne ferme UC3 qu'après le test de bout en bout de l'étape suivante, qui complète la preuve du risque.

Après chaque fusion GitHub :

```powershell
git switch main
git pull --ff-only origin main
```

Les fichiers des UC non encore importés restent localement non suivis. N'exécute pas de commande de nettoyage des fichiers non suivis.

## 7. Importer UC2 et valider l'Élaboration

```powershell
git switch -c feature/uc02-reservation
git add src/main/java/com/chargeslot/service/ReservationEvent.java
git add src/main/java/com/chargeslot/service/ReservationService.java
git add src/main/java/com/chargeslot/controller/ReservationController.java
git add src/test/java/com/chargeslot/ElaborationIntegrationTest.java
git commit -m "feat(UC2): importer la reservation REST de bout en bout"
git push -u origin feature/uc02-reservation
```

Même procédure de PR et revue. La CI doit exécuter ConflictPoliciesTest et ElaborationIntegrationTest : création 201, conflit 409, une seule réservation conservée. UC5 n'est pas encore importé : l'événement de notification est publié mais n'a pas encore de listener ; le test d'Élaboration n'exige pas la notification.

Après fusion, revenir sur main et faire pull. Quand les objectifs Inception ont été effectivement acceptés, taguer le commit de cadrage correspondant avec `v0.0-inception` (identifier son vrai hash avec `git log --oneline`). Pour LCA, après CI distante verte de l'état UC2/UC3 :

```powershell
git tag -a v0.1-elaboration -m "LCA: reservation executable et tests CI verifies"
git push origin v0.1-elaboration
```

Fermer les tickets UC2/UC3 si leurs critères sont vérifiés, puis le milestone `2-elaboration-lca`. Le milestone Inception peut être fermé après validation de son cadrage. Le tag Inception peut se créer avec `git tag -a v0.0-inception HASH_DU_CADRAGE -m "LCO: cadrage valide"`, puis `git push origin v0.0-inception` ; ne pas remplacer le hash par une date inventée.

## 8. Importer les quatre autres UC

Pour **chaque** bloc : partir de main à jour ; créer la branche ; ajouter les seuls fichiers indiqués ; committer ; pousser ; créer une PR liée à la vraie issue ; faire relire et constater la CI ; fusionner ; revenir sur main et pull. Ne pas lancer les quatre blocs sans faire les fusions entre eux.

### UC1

```powershell
git switch -c feature/uc01-disponibilites
git add src/main/java/com/chargeslot/controller/AvailabilityController.java
git commit -m "feat(UC1): exposer la recherche de disponibilites"
git push -u origin feature/uc01-disponibilites
```

### UC4

```powershell
git switch -c feature/uc04-annulation
git add src/main/java/com/chargeslot/controller/CancellationController.java
git commit -m "feat(UC4): exposer l'annulation autorisee et idempotente"
git push -u origin feature/uc04-annulation
```

Le service partagé ReservationService contient déjà la méthode d'annulation ; cette branche expose son cas d'utilisation.

### UC5

```powershell
git switch -c feature/uc05-notifications
git add src/main/java/com/chargeslot/service/NotificationGateway.java
git add src/main/java/com/chargeslot/service/LocalNotificationGateway.java
git add src/main/java/com/chargeslot/service/NotificationService.java
git add src/main/java/com/chargeslot/controller/NotificationController.java
git commit -m "feat(UC5): livrer et tracer les notifications locales apres commit"
git push -u origin feature/uc05-notifications
```

### UC6 et tests complets

```powershell
git switch -c feature/uc06-infrastructure
git add src/main/java/com/chargeslot/service/InfrastructureService.java
git add src/main/java/com/chargeslot/controller/InfrastructureController.java
git add src/test/java/com/chargeslot/ReservationIntegrationTest.java
git commit -m "feat(UC6): administrer l'infrastructure et verifier les six UC"
git push -u origin feature/uc06-infrastructure
```

Les tests d'intégration complets vérifient aussi UC1/UC4/UC5 et les réservations concurrentes. Attendre cette validation complète avant de considérer tous les critères terminés. Les preuves des tickets peuvent pointer vers les tests et la PR correspondante.

Après fusion UC6 et CI verte :

```powershell
git tag -a v0.2-construction -m "IOC: six UC implementes et tests verifies"
git push origin v0.2-construction
```

Fermer le milestone Construction lorsque ses tickets et critères sont effectivement validés.

## 9. Interface, Transition et revue externe

```powershell
git switch -c feature/interface-documentation
git add src/main/resources/static
git commit -m "feat: ajouter l'interface locale de demonstration"
git push -u origin feature/interface-documentation
```

Faire la PR et sa revue, puis fusionner. Tester la démonstration. Demander à une **autre équipe** de remplir REVUE-CROISEE.md. Reporter leurs observations réelles, créer les tickets de correction et traiter les remarques. Ajouter la grille réellement remplie dans un commit/une PR séparée.

Quand la revue externe et les corrections sont acceptées, la CI est verte et la démonstration fonctionne :

```powershell
git switch main
git pull --ff-only origin main
git tag -a v1.0.0 -m "Product Release: revue externe et demonstration validees"
git push origin v1.0.0
```

Fermer le milestone Transition. Le numéro 1.0.0 du logiciel local n'est pas, à lui seul, la preuve que ce jalon a été approuvé.

## 10. Vérifier la CI

Dans **Actions**, ouvrir « Java 21 - build et tests », choisir le dernier run de la branche/PR, puis le job `verify`. Il installe Java 21 et exécute `./mvnw verify`. Les rapports sont conservés dans l'artifact `resultats-tests`.

La réussite locale est fournie ; **aucune CI distante n'a été lancée dans cette session**. Un run vert distant doit être constaté après ton push. Si le run échoue, lire la première erreur utile dans le job avant de fusionner ; ne pas poser le tag LCA pour masquer l'échec.

## 11. Contrôle final avant remise

- [ ] Six issues UC avec responsables et critères.
- [ ] Quatre milestones et tableau de bord renseignés.
- [ ] Une branche et une PR identifiables par UC.
- [ ] Sources PlantUML committées avec les rendus.
- [ ] Tests locaux et CI distante verts.
- [ ] Quatre tags de jalons présents et associés à des états vérifiés.
- [ ] Revue réellement faite par une autre équipe et corrections suivies.
- [ ] README utilisable par un autre étudiant, noms réels complétés.
- [ ] `git status` ne révèle pas de fichier source oublié (les JAR, target et data sont volontairement ignorés).

## Références GitHub officielles

- [Gérer les remotes](https://docs.github.com/en/get-started/git-basics/managing-remote-repositories).
- [Créer les milestones](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/creating-and-editing-milestones-for-issues-and-pull-requests).
- [Créer un projet Board](https://docs.github.com/en/issues/planning-and-tracking-with-projects/creating-projects/creating-a-project).
- [Compiler et tester Java/Maven dans Actions](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven).
