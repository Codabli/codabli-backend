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
modification de l'historique et la lecture/écriture JPA. Hibernate ne crée,
ne modifie et ne valide pas les tables (`ddl-auto: none`) ; seul Liquibase gère
le DDL. H2 ne remplace pas une vérification du déploiement sur PostgreSQL :
les différences de types et de dialectes restent à prendre en compte.

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

| Famille | Annotation | Base requise ? | Rôle |
| --- | --- | :---: | --- |
| Tests de **contrôleurs** | `@SpringBootTest` + `@AutoConfigureMockMvc` | H2 en mémoire | Vérifient les endpoints HTTP (routes, statuts, sécurité par rôle) avec le contexte Spring réel |
| Tests de **services** | `@ExtendWith(MockitoExtension.class)` | ❌ Non | Vérifient la logique métier isolée (dépôt mocké) : règles d'accès, filtres, calculs |
| Tests de **migrations** | `@SpringBootTest` | H2 en mémoire | Vérifient schéma dédié, idempotence Liquibase et persistance JPA |

### Inventaire

**Contrôleurs — `@SpringBootTest` (H2 en mémoire) — 27 tests**

| Classe | Nb tests |
| --- | :---: |
| `RessourcePedagogiqueControllerTest` | 8 |
| `GalerieControllerTest` | 6 |
| `ActualiteControllerTest` | 5 |
| `CommandeControllerTest` | 5 |
| `ConteDanseControllerTest` | 3 |

**Services — Mockito (pas de BDD) — 32 tests**

| Classe | Nb tests |
| --- | :---: |
| `RessourcePedagogiqueServiceTest` | 9 |
| `GalerieServiceTest` | 6 |
| `PanierServiceTest` | 6 |
| `ProduitServiceTest` | 6 |
| `CommandeServiceTest` | 3 |
| `ActualiteServiceTest` | 1 |
| `ConteDanseServiceTest` | 1 |

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

| Symptôme                                                                    | Cause probable | Solution                                                     |
|-----------------------------------------------------------------------------| --- |--------------------------------------------------------------|
| Les tests tentent une connexion à PostgreSQL | Configuration de test surchargée | Vérifier les variables `SPRING_DATASOURCE_*` et la configuration IDE ; H2 est le défaut |
| `release version 25 not supported`                                          | Pas de JDK (seulement un JRE) sur l'hôte | Utiliser l'**option B** (tout Docker) ou installer un JDK 25+ |
| `UnnecessaryStubbingException`                                              | Stub partagé non utilisé par tous les tests | Passer le stub en `lenient().when(...)`                      |
