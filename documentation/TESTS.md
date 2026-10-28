# 🧪 Tests — Codabli Backend

Guide d'exécution et d'organisation de la suite de tests. Pour l'installation
générale et le lancement de l'application, voir le [README principal](../README.md).

---

## Base de données des tests

Les tests de contrôleurs et de migrations chargent le contexte Spring complet
et utilisent **H2 en mémoire**, configuré dans `src/test/resources/application.yml`.
Le schéma `codabli_test` est créé à la connexion, puis Liquibase exécute le même
changelog que l'application. Aucun serveur PostgreSQL ou Keycloak n'est requis,
en local comme en CI. Les tests de services Mockito n'ont pas besoin de base.

`LiquibaseMigrationTest` vérifie le schéma dédié, une seconde application sans
modification de l'historique, la lecture/écriture JPA des produits et messages IA,
ainsi que la purge de l'historique a la publication (commit et rollback).
Il couvre aussi les tours concurrents, leur ordre question/reponse, la
publication pendant un ajout et les filtres/traductions du catalogue sur
de vraies tables.

`LiquibaseUpgradeTest` couvre installation neuve, mise a niveau depuis 001 avec
conservation des donnees et checksums, index, rejet des messages orphelins,
suppression en cascade et idempotence. Hibernate ne crée,
ne modifie et ne valide pas les tables (`ddl-auto: none`), seul Liquibase gère
le DDL. H2 ne remplace pas une vérification du déploiement sur PostgreSQL :
les différences de types et de dialectes restent à prendre en compte.

Les tests `@DataJpaTest` de l'historique conservent la datasource configuree
avec `@AutoConfigureTestDatabase(replace = NONE)` : la remplacer par une H2
automatique ferait perdre l'initialisation du schema `codabli_test`.

### Verifier les migrations sur PostgreSQL 16

Utiliser une **base jetable dediee**, jamais une base de recette ou partagee :

```bash
./mvnw test -Dtest=LiquibaseUpgradeTest \
  -Dmigration.test.url=jdbc:postgresql://localhost:55432/migration_test \
  -Dmigration.test.username=migration_test \
  -Dmigration.test.password=migration_test
```

Le test cree un schema aleatoire pour chaque scenario et supprime uniquement
ce schema a la fin. Le role doit pouvoir creer des schemas. La suite ordinaire
reste autonome sur H2 et ne necessite pas Docker.

Pour verifier tous les mappings Hibernate sur PostgreSQL, provisionner un
schema `codabli_test` dans cette meme base jetable, puis lancer
`LiquibaseMigrationTest` en surchargeant `spring.datasource.url` (avec
`?currentSchema=codabli_test`), `spring.datasource.username`,
`spring.datasource.password`, `spring.datasource.driver-class-name`
(`org.postgresql.Driver`) et `spring.jpa.hibernate.ddl-auto=validate`.
Ne pas activer cette validation sur H2 : Liquibase y traduit `TEXT` en CLOB,
alors que le mapping Hibernate attend VARCHAR, sans incompatibilite PostgreSQL.

---

## ▶️ Lancer les tests

### Option A — natif (nécessite un JDK 25+)

```bash
./mvnw test
```

### Option B — tout Docker (aucun JDK requis sur l'hôte)

Si la machine n'a qu'un JRE (pas de `javac`), on compile/exécute les tests dans un
conteneur Maven + JDK 25 :

```bash
docker run --rm \
  -v "$PWD":/app -w /app \
  maven:3.9-eclipse-temurin-25 mvn -B test
```

### Commandes utiles

```bash
# Une seule classe de test
./mvnw test -Dtest=RessourcePedagogiqueServiceTest

# Une seule méthode
./mvnw test -Dtest=RessourcePedagogiqueServiceTest#creer_shouldSaveRessource

# Continuer malgré les échecs et voir le récapitulatif complet
./mvnw test -Dmaven.test.failure.ignore=true
```

Les rapports détaillés sont générés dans **`../target/surefire-reports`**.

---

## 🗂️ Organisation de la suite

La suite comporte les familles suivantes :

| Famille                    | Annotation                                  |       Base requise ?        | Rôle                                                                                           |
|----------------------------|---------------------------------------------|:---------------------------:|------------------------------------------------------------------------------------------------|
| Tests de **contrôleurs**   | `@SpringBootTest` + `@AutoConfigureMockMvc` |        H2 en mémoire        | Vérifient les endpoints HTTP (routes, statuts, sécurité par rôle) avec le contexte Spring réel |
| Tests de **services**      | `@ExtendWith(MockitoExtension.class)`       |           ❌ Non            | Vérifient la logique métier isolée (dépôt mocké) : règles d'accès, filtres, calculs            |
| Tests de **migrations**    | `@SpringBootTest`                           |        H2 en mémoire        | Vérifient schéma dédié, idempotence Liquibase et persistance JPA                               |
| Tests de **mise a niveau** | JUnit + Liquibase/JDBC                      | H2, ou PostgreSQL explicite | Verifient migration additive, conservation des donnees et contraintes reelles                  |
| Tests de **repositories**  | `@DataJpaTest`                              |        H2 en memoire        | Verifient les requetes sur le schema cree par Liquibase                                        |

---

## 🧩 Conventions

- **Mockito en mode strict** (par défaut avec `MockitoExtension`) : un *stub*
  déclaré mais jamais utilisé fait échouer le test (`UnnecessaryStubbingException`).
  Pour des *fixtures partagées* dans `@BeforeEach` que seuls certains tests
  utilisent (ex. plusieurs JWT de rôles différents), on déclare le stub en
  `lenient().when(...)` — ainsi la strictness reste active partout ailleurs.
- Les tests de services **ne doivent pas** dépendre d'une base : le dépôt est
  toujours mocké. Si un test de service a besoin de la base, c'est le signe qu'il
  devrait être un test d'intégration/contrôleur.

---

## 🩺 Dépannage

| Symptôme                                     | Cause probable                              | Solution                                                                               |
|----------------------------------------------|---------------------------------------------|----------------------------------------------------------------------------------------|
| Les tests tentent une connexion à PostgreSQL | Configuration de test surchargée            | Vérifier les variables `SPRING_DATASOURCE_*` et la configuration IDE, H2 est le défaut |
| `release version 25 not supported`           | Pas de JDK (seulement un JRE) sur l'hôte    | Utiliser l'**option B** (tout Docker) ou installer un JDK 25+                          |
| `UnnecessaryStubbingException`               | Stub partagé non utilisé par tous les tests | Passer le stub en `lenient().when(...)`                                                |
