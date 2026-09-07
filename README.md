# 💻 Codabli Backend

Backend de l'application **Codabli**, développé en **Spring Boot 3.3.2** et **Java 17**. Ce service assure la gestion des utilisateurs, de la scolarité, du catalogue de contes, de l'actualité, de la modération des cartes créées par les étudiants ainsi que d'une boutique e-commerce.

---

## 🛠️ Stack Technique

* **Framework principal** : Spring Boot 3.3.2
* **Persistance** : Spring Data JPA / Hibernate / PostgreSQL
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
* **JDK 17 ou +** *(optionnel)* — seulement pour le lancement natif via `./mvnw`.
  Si tu n'as qu'un JRE, utilise la variante « tout Docker » ci-dessous.
* Maven n'a **pas** besoin d'être installé : le wrapper `./mvnw` s'en charge.

### Services & ports

| Service        | URL / port hôte                         | Identifiants          |
| -------------- | --------------------------------------- | --------------------- |
| API backend    | http://localhost:8082                   | —                     |
| Swagger UI     | http://localhost:8082/swagger-ui.html   | —                     |
| PostgreSQL     | `localhost:5442` (base `codabli_dev`)   | `codabli` / `codabli` |
| Keycloak admin | http://localhost:8081                   | `admin` / `admin`     |

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

Cela lance PostgreSQL et Keycloak. Le realm `codabli` (rôles, client
`codabli-backend`, service account) est importé automatiquement au premier
démarrage.

### 2. Démarrer le backend

**Option A — natif (recommandé, nécessite un JDK 17+)**
```bash
./mvnw spring-boot:run
```

**Option B — tout Docker (aucun JDK requis)**
```bash
docker compose --profile app up -d
```
Le backend est alors compilé et exécuté dans un conteneur Maven+JDK 17.

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

> ⚠️ **Prérequis : PostgreSQL doit être démarré.** Les tests de contrôleurs sont
> des `@SpringBootTest` : ils chargent le contexte Spring complet et se
> connectent à la base sur `localhost:5442`. Sans base, ils échouent tous avec
> `Connection to localhost:5442 refused`. Démarre l'infrastructure avant :
> ```bash
> docker compose up -d postgres
> ```

Pour lancer la suite de tests unitaires et d'intégration (nécessite un JDK 17+) :
```bash
./mvnw test
```
*(Vous pouvez également importer le projet sous IntelliJ IDEA pour compiler et exécuter les tests directement depuis l'IDE)*.

👉 **Guide complet des tests** (organisation, exécution sans JDK, inventaire,
conventions, dépannage) : **[TESTS.md](TESTS.md)**.

---

## 🩺 Dépannage

* **`release version 17 not supported` au build** : seul un **JRE** est installé,
  pas de JDK (pas de `javac`). Installe un JDK — p. ex. sur Ubuntu/WSL :
  `sudo apt install openjdk-21-jdk` — ou utilise l'**option B** (tout Docker),
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
documenté dans **[MODULES.md](MODULES.md)**.

Documentation complémentaire :
* [`DOC_FONCTIONNELLE.md`](DOC_FONCTIONNELLE.md) — spécification fonctionnelle détaillée.
* [`FICHE_TECHNIQUE.md`](FICHE_TECHNIQUE.md) — fiche technique (entités, sécurité, endpoints).
* [`TESTS.md`](TESTS.md) — guide d'exécution et d'organisation des tests.