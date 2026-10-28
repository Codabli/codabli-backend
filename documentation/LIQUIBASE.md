# Liquibase et schema PostgreSQL dédié

## Fonctionnement

Le schema par défaut de l'application est `codabli`, configuré par
`POSTGRES_SCHEMA`.

L'initialisation comporte deux étapes :

1. **Provisioning** : `infrastructure/db/init-schema.sql` créé le schema s'il
   n'existe pas, avec l'utilisateur connecté comme propriétaire. C'est un script
   **psql**, pas un changelog Liquibase. Il ne supprime rien. L'utilisateur
   doit avoir le droit `CREATE` sur la base pour une première création.
2. **Demarrage du backend** : Liquibase applique les migrations dans ce schema.
   Hibernate ne créé, ne modifie et ne valide pas la structure
   (`ddl-auto: none`). Seul Liquibase gere le DDL.

Les tables `databasechangelog` et `databasechangeloglock` sont aussi créées dans
le schema dédié. La première conserve les migrations exécutées et leurs
checksums, la seconde empêche deux instances de migrer simultanément.
Au redémarrage, les migrations déjà appliquées ne sont pas rejouées.

## Développement local

Copier `.env.example` vers `.env`, puis lancer `docker compose up -d`.
Le service ponctuel `postgres-init` prépare le schema après le démarrage de
PostgreSQL, même sur un volume existant. Le backend du profil `app` attend
la réussite de ce service avant de démarrer.

Ce service n'est pas une seconde base : il lance simplement le client `psql`,
exécute le script puis s'arrête. Il ne lit ni ne copie les données de `public`.
Il évite une création manuelle du schema en local, un script dans
`docker-entrypoint-initdb.d` ne serait joué que sur un volume PostgreSQL neuf,
alors que ce service fonctionne aussi sur un volume déjà initialisé.

Pour relancer uniquement le provisioning :

```bash
docker compose run --rm postgres-init
```

## Première bascule en recette (données non conservées)

La recette est gérée par **Codabli/codabli-infra**, pas par le Compose local de
ce dépôt. Ces opérations nécessitent un accès à la VM et à la base. Le merge
du backend ne les effectue pas. Ne pas supprimer les volumes PostgreSQL ou
Keycloak : seul le stockage applicatif est concerné.

1. **Arrêter le backend avant le déploiement.** Dans le répertoire Compose de
   recette : `docker compose stop backend`. Les données de `public` sont
   explicitement abandonnées : aucune sauvegarde, copie ou baseline n'est
   nécessaire pour cette bascule. Vérifier seulement que les objets à supprimer
   appartiennent bien à Codabli.

2. Dans le `.env` de recette, définir `POSTGRES_SCHEMA=codabli`.
   Dans le service `backend` du Compose de recette, transmettre cette variable
   au conteneur. Si l'URL JDBC est surchargée (cas habituel en Docker), aligner
   son `currentSchema` sur cette même valeur :

   ```yaml
   environment:
     POSTGRES_SCHEMA: ${POSTGRES_SCHEMA:-codabli}
     SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}?currentSchema=${POSTGRES_SCHEMA:-codabli}
   ```

   Conserver les autres variables existantes, notamment les identifiants.
   Adapter le nom du service PostgreSQL et la base au Compose réel.

3. Copier `infrastructure/db/init-schema.sql` de cette branche dans le répertoire
   de recette sous le nom `init-schema.sql`, puis exécuter, avec le même rôle
   PostgreSQL que le backend :

   ```bash
   docker compose exec -T postgres sh -ec \
     'psql --no-psqlrc --set=ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" --set=schema=codabli' \
     < init-schema.sql
   ```

   Adapter `schema=codabli` si un autre nom a été choisi. Si les identifiants
   internes du service PostgreSQL ne sont pas ceux du backend, utiliser le rôle
   applicatif explicitement. Un administrateur peut aussi créer le schema avec
   `CREATE SCHEMA codabli AUTHORIZATION <role_applicatif>;`.
   La commande de provisioning est idempotente, mais elle ne corrige pas les
   droits d'un schema preexistant : vérifier son propriétaire dans ce cas.

4. Le nouveau schema doit être vide avant la première migration. L'application
   cible désormais `codabli` et ne reprend aucune donnée de `public`.
   **Ne pas utiliser `changelog-sync`** pour cette installation neuve.

5. Déployer l'image de cette branche avec le Compose de recette modifié :

   ```bash
   docker compose pull backend
   docker compose up -d --no-build backend
   docker compose logs --tail=100 backend
   ```

   Vérifier le succès des migrations, le démarrage Hibernate et les endpoints
   publics. Contrôler dans la base :

   ```sql
   SELECT count(*) FROM codabli.databasechangelog;
   SELECT * FROM codabli.databasechangeloglock;
   SELECT table_name FROM information_schema.tables WHERE table_schema = 'codabli';
   ```

   Pour le changelog actuel, 100 changesets doivent être enregistrés
   (97 pour le schema initial et 3 pour les medias),
   et le verrou doit être libéré. Redémarrer le backend et vérifier que
   Liquibase ne réapplique aucun changeset.

6. **Nettoyer `public` seulement après vérification.** Identifier les anciennes
   tables Codabli et leurs dépendances, ne pas supprimer des objets partagés,
   extensions ou objets Keycloak. Cette requête génère une liste à examiner,
   elle ne supprime rien :

   ```sql
   SELECT format('DROP TABLE public.%I;', tablename)
   FROM pg_tables
   WHERE schemaname = 'public'
     AND tablename IN (
       SELECT tablename FROM pg_tables WHERE schemaname = 'codabli'
     )
   ORDER BY tablename;
   ```

   Exécuter ensuite les suppressions confirmées dans une transaction, en tenant
   compte des clés étrangères entre anciennes tables. Ne pas ajouter `CASCADE`
   sans examiner les dépendances. Il n'est pas nécessaire de supprimer le schema
   `public` lui-même et aucun nettoyage n'est automatisé au démarrage.

Le retour à l'ancienne image n'est pas une stratégie de reprise des données :
elle utilisait `public`. Si la bascule échoue, garder le backend arrêté,
corriger l'erreur et relancer la migration avant tout nettoyage.

## Rôle des fichiers dans src/main/resources/db

| Fichier                                   | Rôle                                                                                                                                                   | Exécute automatiquement ?                           |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| `changelog/db.changelog-master.xml`       | Point d'entree, liste les migrations a executer dans l'ordre.                                                                                          | Oui, au demarrage du backend.                       |
| `changelog/001-schema.xml`                | Structure initiale : tables, colonnes, cles primaires, contraintes uniques et cles etrangeres. Aucun jeu de donnees metier.                            | Oui, via le master, une seule fois par changeset.   |
| `changelog/002-media.xml`                 | Metadonnees des médias S3, sans stockage du contenu binaire en base.                                                                                   | Oui.                                                |
| `changelog/003-ressource-media.xml`       | Relation nullable des ressources pédagogiques vers les médias, sans modifier `fichier_url`.                                                            | Oui.                                                |
| `changelog/004-media-cleanup-index.xml`   | Index des états et dates pour le nettoyage asynchrone.                                                                                                 | Oui.                                                |
| `changelog/sql/seed-data.sql`             | Jeu de demonstration : utilisateurs locaux, contes, produits, etc. Ne cree pas de comptes Keycloak.                                                    | Non, chargement manuel facultatif en developpement. |
| `changelog/sql/fix-check-constraints.sql` | Ancien correctif manuel des contraintes d'enumeration des bases creees par Hibernate. Conserve comme historique, inutile sur le schema Liquibase neuf. | Non.                                                |
| `changelog/sql/schema.json`               | Ancien export descriptif partiel du schema, ni migration ni source de verite.                                                                          | Non.                                                |

Spring Boot embarque les ressources dans le JAR, mais ne joue pas tous les
fichiers SQL presents : seuls les fichiers references par le changelog actif
sont executes par Liquibase. Les trois fichiers du sous-dossier `sql` ne sont
pas references par le master.

Pour charger le seed, se connecter a la bonne base, puis executer
`SET search_path TO codabli;` dans la meme session avant le script.
Le seed reste volontairement hors du changelog pour ne jamais injecter de
donnees de demonstration automatiquement en recette.

## Prochaines evolutions

Ajouter un nouveau changelog (par exemple `005-ajout-colonne.xml`) et l'inclure
apres `004-media-cleanup-index.xml` dans le master. Ne pas modifier un changeset deja applique :
son checksum est enregistré en base. Ne pas reinitialiser les tables de suivi
pour contourner une erreur de checksum.

Les tests utilisent H2 en memoire dans le schema `codabli_test`, sans serveur
externe. Ils verifient la persistance et l'idempotence du meme changelog,
sans validation de schema par Hibernate. La verification du deploiement
sur PostgreSQL reste necessaire, car les dialectes et certains types different. Voir
[TESTS.md](TESTS.md).

## Ajout des medias sur une base deja migree depuis develop

Le changelog initial `001-schema.xml` reste identique a celui de develop.
Sur une base dont les 97 changesets initiaux ont deja ete appliques dans le
schema dédié, Liquibase execute uniquement les trois migrations medias
suivantes, sans baseline ni `changelog-sync`. Les tables de suivi et les
nouvelles tables restent dans le schema configure par `POSTGRES_SCHEMA`.

Ces migrations sont additives. Elles préservent les données métier et les
URL historiques. Ne pas modifier les checksums ni réinitialiser l'historique
Liquibase pour les appliquer. La procédure de première bascule depuis
`public` reste celle décrite ci-dessus, et non une reprise du schema
Hibernate historique.
