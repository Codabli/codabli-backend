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

## 📂 Modules Fonctionnels Implémentés

### 🔐 1. Sécurité & Utilisateurs
* Double inscription et synchronisation locale depuis le jeton **Keycloak** (JWT). Mappage des rôles du Realm (`admin`, `eleve`, `enseignant`, `parent`, etc.).
* Habilitation fine à 2 niveaux : `@PreAuthorize` au niveau contrôleur & validation logique métier spécifique dans les services.

### 🏫 2. Scolarité (Écoles, Classes & Inscriptions)
* Gestion des établissements scolaires (`Ecole`) et des classes (`Classe`) avec code d'invitation unique.
* Table d'inscription des élèves dans les classes.

### 📰 3. Actualités (`/api/actualites`)
* Flux d'actualités avec pagination et filtrage par statut de publication. CRUD complet restreint aux administrateurs.

### 📖 4. Contes Dansés (`/api/contes-danses`)
* Catalogue des contes avec pièces jointes (texte, audio et vidéo). Création par tout utilisateur connecté, modification restreinte à l'auteur ou l'admin.

### 🎨 5. Cartes à Conte & Modération (`/api/cartes-a-conte`)
* Création d'éléments de contes (personnages, lieux, objets).
* **Flux de modération** : les élèves créent leurs cartes, les enseignants valident ou rejettent les cartes uniquement pour les élèves inscrits dans leurs propres classes, et les admins ont un droit total.

### 🔍 6. Recherche Transversale (`/api/recherche`)
* Endpoint unique permettant d'effectuer des recherches globales insensibles à la casse simultanément dans les actualités et les contes.

### 🛒 7. Boutique & E-Commerce (Produits, Panier & Commandes)
* **Produits** : Gestion des articles physiques et numériques (GET public, modification admin).
* **Panier** : Gestion persistée par utilisateur connecté avec calcul automatique des totaux.
* **Commande** : Passage de commande transactionnel avec :
  1. Décrémentation automatique des stocks physiques (déclenche `StockInsuffisantException` / 409 Conflict si stock insuffisant).
  2. Snapshotting (historisation) des prix unitaires et noms de produits pour conserver l'historique financier.
  3. Intégration d'adresses de livraison (gestion d'adresse par défaut).
  4. Calcul automatique de frais de port (5.00€ si présence d'au moins un article physique).
  5. Vidage de panier automatique après confirmation.

### 🎒 8. Mallette Pédagogique (`/api/ressources-pedagogiques`)
* Catalogue de matériel pédagogique (fiches, guides, vidéos, audio, documents) réservés aux enseignants.
* **Sécurité & Visibilité** :
  * `GET` pour `enseignant`, `professionnel_education`, `admin` et `comite_lecture`. Filtrage dynamique : les rôles hors-staff ne peuvent voir que les ressources actives (`actif = true`).
  * `POST/PUT` réservés aux rôles `admin` et `comite_lecture`.
  * `DELETE` réservé exclusivement aux administrateurs (`admin`).


---

## 🚀 Lancement local

### Pré-requis
1. Disposer d'une base de données PostgreSQL nommée `codabli_dev`.
2. Avoir une instance Keycloak configurée (realm `codabli`).

### Exécution
```bash
mvn spring-boot:run
```

L'API est ensuite accessible sur : `http://localhost:8080`.
La documentation Swagger est disponible sur: `http://localhost:8080/swagger-ui.html`

---

## 🧪 Exécution des tests

Pour lancer la suite de tests unitaires et d'intégration :
```bash
mvn test
```
*(Vous pouvez également importer le projet sous IntelliJ IDEA pour compiler et exécuter les tests directement depuis l'IDE)*.