# 💻 Codabli Backend

Backend de l'application **Codabli**, développé en **Spring Boot 4.1.1** et **Java 25**. Ce service assure la gestion des utilisateurs, de la scolarité, du catalogue de contes, de l'actualité, de la modération des cartes créées par les étudiants ainsi que d'une boutique e-commerce.

L'application est disponible en recette, lien pour le swagger : https://recette.codabli.com/swagger-ui/index.html

---

## 🛠️ Stack Technique

* **Framework principal** : Spring Boot 4.1.1
* **Persistance** : Spring Data JPA / Hibernate / PostgreSQL, migrations Liquibase
* **Sécurité & IAM** : Spring Security & Keycloak (OAuth2 Resource Server)
* **Outillage & Doc** : Lombok, Springdoc OpenAPI (Swagger UI)
* **Tests** : JUnit 5, Mockito, MockMvc

---

## 🚀 Lancement local

L'environnement de développement (PostgreSQL + Keycloak déjà configurés) est
fourni via **Docker Compose**. Aucune configuration manuelle de la base ou du
realm n'est nécessaire : tout est provisionné automatiquement.

### Pré-requis
* **Docker** + **Docker Compose** (obligatoire — fournit PostgreSQL et Keycloak).
* **JDK 25 ou +** *(optionnel)* — seulement pour le lancement natif via `./mvnw`.
  Si tu n'as qu'un JRE, utilise la variante « tout Docker » ci-dessous.
* Maven n'a **pas** besoin d'être installé : le wrapper `./mvnw` s'en charge.

### Services & ports

| Service        | URL / port hôte                       | Identifiants          |
| -------------- |---------------------------------------| --------------------- |
| API backend    | http://localhost:8082                 | —                     |
| Swagger UI     | http://localhost:8082/swagger-ui.html | —                     |
| PostgreSQL     | `localhost:5442` (base `codabli_dev`) | `codabli` / `codabli` |
| Keycloak admin | http://localhost:8081               | `admin` / `admin`     |

> ℹ️ PostgreSQL est exposé sur **5442** (et non 5432) car le port 5432 de l'hôte
> est susceptible d'être déjà occupé par un autre projet. `application.yml` est
> configuré en conséquence.

### 0. Créer le fichier `.env`

Les identifiants (base, Keycloak, secret du client) sont lus depuis un fichier
`.env` **non versionné**. Copie le modèle avant tout :

```bash
cp .env.example .env
```

Les valeurs par défaut conviennent pour un environnement de dev local.

### 1. Démarrer l'infrastructure

```bash
docker compose up -d
```

Cela lance PostgreSQL et Keycloak. Le service ponctuel `postgres-init` crée
le schéma dédié défini par `POSTGRES_SCHEMA` (`codabli` par défaut), y compris
si le volume PostgreSQL existe déjà. Le realm `codabli` (rôles, client
`codabli-backend`, service account) est importé automatiquement au premier
démarrage.

### 2. Démarrer le backend

**Option A — natif (recommandé, nécessite un JDK 25+)**
```bash
./mvnw spring-boot:run
```

**Option B — tout Docker (aucun JDK requis)**
```bash
docker compose --profile app up -d
```
Le backend est alors compilé et exécuté dans un conteneur Maven+JDK 25.

Au démarrage, Liquibase crée les tables et enregistre les migrations dans le
schéma dédié. Hibernate ne crée, ne modifie et ne valide pas la structure
de la base (`ddl-auto: none`).

Pour un lancement natif, Spring Boot ne charge pas automatiquement le fichier
`.env` : exporter les variables personnalisées dans le terminal ou les définir
dans la configuration de lancement de l'IDE. Les valeurs par défaut correspondent
à `.env.example`.

L'API est accessible sur **http://localhost:8082**, la documentation Swagger sur
**http://localhost:8082/swagger-ui.html**.

### 3. Se connecter

Un compte administrateur de test est créé automatiquement dans Keycloak :

| Email               | Mot de passe | Rôles                |
| ------------------- | ------------ | -------------------- |
| `admin@codabli.dev` | `Admin1234!` | `admin`, `super_admin` |

Pour obtenir un jeton via l'API :
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@codabli.dev","password":"Admin1234!"}'
```

> Ce compte existe dans Keycloak mais pas dans la base locale : les endpoints
> protégés par rôle (`/api/admin/**`, etc.) fonctionnent avec son jeton, mais
> `GET /api/auth/me` renverra 404 tant qu'aucune ligne locale ne lui correspond.
> Pour un compte pleinement lié (Keycloak **et** base locale), crée-le via
> `POST /api/auth/register` — c'est le parcours d'inscription normal.

### Commandes utiles

```bash
docker compose logs -f keycloak     # suivre les logs Keycloak
docker compose logs -f backend      # logs du backend (si lancé en Docker)
docker compose down                 # arrêter (les données sont conservées)
docker compose down -v              # arrêter ET repartir de zéro (efface DB + realm)
```

---

## 🧪 Exécution des tests

Les tests d'intégration utilisent **H2 en mémoire**, dans un schéma dédié
initialisé par la configuration de test. Liquibase y exécute le même changelog
que l'application, avec `ddl-auto: none`.
Aucun serveur PostgreSQL ou Keycloak n'est nécessaire, en local comme en CI.

Pour lancer la suite de tests unitaires et d'intégration (nécessite un JDK 25+) :
```bash
./mvnw test
```
*(Vous pouvez également importer le projet sous IntelliJ IDEA pour compiler et exécuter les tests directement depuis l'IDE)*.

👉 **Guide complet des tests** (organisation, exécution sans JDK, inventaire,
conventions, dépannage) : **[TESTS.md](documentation/TESTS.md)**.

---

## 🩺 Dépannage

* **`release version 25 not supported` au build** : seul un **JRE** est installé,
  pas de JDK (pas de `javac`). Installe un JDK — p. ex. sur Ubuntu/WSL :
  `sudo apt install openjdk-25-jdk` — ou utilise l'**option B** (tout Docker),
  qui n'exige aucun JDK sur l'hôte.
* **Port 5432 déjà utilisé** : c'est prévu — l'infra Codabli utilise **5442**.
  Vérifie qu'aucun autre service ne squatte 5442, 8081 ou 8082.
* **Keycloak « injoignable » juste après `up`** : son premier démarrage +
  import du realm prend ~20-40 s. Surveille `docker compose logs -f keycloak`.
* **Repartir totalement de zéro** : `docker compose down -v` supprime les
  volumes (base et realm sont alors réimportés au prochain `up`).

---

## 📂 Modules fonctionnels

Le détail des modules fonctionnels du backend et de leurs règles d'accès est
documenté dans **[MODULES.md](documentation/MODULES.md)**.

Documentation complémentaire :
* [`documentation/LIQUIBASE.md`](documentation/LIQUIBASE.md) — schéma dédié, rôle des fichiers DB et bascule de la recette.
* [`documentation/DOC_FONCTIONNELLE.md`](documentation/DOC_FONCTIONNELLE.md) — spécification fonctionnelle détaillée.
* [`documentation/FICHE_TECHNIQUE.md`](documentation/FICHE_TECHNIQUE.md) — fiche technique (entités, sécurité, endpoints).
* [`documentation/TESTS.md`](documentation/TESTS.md) — guide d'exécution et d'organisation des tests.