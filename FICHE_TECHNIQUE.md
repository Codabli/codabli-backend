# Fiche Technique - Backend Codabli

Ce document récapitule l'ensemble des modules, entités, APIs et configurations de sécurité actuellement implémentés dans le projet backend **Codabli**.

---

## 🛠️ Architecture Technique & Dépendances

Le backend est développé sous **Spring Boot 4.1.1** et **Java 25** avec les technologies clés suivantes :
1. **Spring Data JPA & PostgreSQL** : Persistance des données locales (base `codabli_dev`).
2. **Spring Security & Keycloak (OAuth2 Resource Server & Admin Client)** : Authentification unifiée et contrôle d'accès. Retrait des tokens JWT et synchronisation logique des utilisateurs dans la base locale.
3. **Lombok** : Réduction du code boilerplate (builders, getters/setters).
4. **Springdoc OpenAPI (Swagger UI)** : Documentation de l'API disponible sur `/swagger-ui.html` et `/v3/api-docs`.

---

## 🔐 Sécurité & Gestion des Rôles

L'authentification repose sur des jetons JWT émis par **Keycloak** (realm `codabli`).
* **KeycloakJwtConverter** : Convertit et mappe les rôles du realm Keycloak en `ROLE_<nom_role>` (ex: `ROLE_admin`, `ROLE_eleve`, `ROLE_enseignant`, `ROLE_parent`) exploitables par Spring Security.
* **Sécurité en 2 niveaux** :
  1. **Niveau Global / Contrôleur** : `@PreAuthorize("hasRole('...')")` valide le rôle utilisateur brut.
  2. **Niveau Fin / Service** : Vérification des relations spécifiques (ex: un enseignant ne peut valider que la carte d'un élève inscrit dans une de ses classes).

### Règles d'Accès aux Endpoints :
* **Endpoints Publics** :
  * `POST /api/auth/register` (création de compte)
  * `POST /api/auth/login` (récupération de token)
  * `GET /api/actualites/**` (lecture des actualités)
  * `GET /api/contes-danses/**` (catalogue des contes)
  * `GET /api/recherche` (recherche globale)
  * Config & metadata : `/api/ecoles/**`, `/api/classes/**`, `/api/inscriptions-classes/**`
  * Documentation : `/swagger-ui/**`, `/v3/api-docs/**`
* **Endpoints Administrateur** (`hasRole('admin')`) :
  * `DELETE /api/admin/utilisateurs/{id}`
  * `GET /api/admin/utilisateurs/**`
  * `PATCH /api/admin/utilisateurs/{id}/statut`
* **Endpoints Authentifiés** (`anyRequest().authenticated()`) :
  * `GET/PUT /api/auth/me` (profil utilisateur)
  * `POST/PUT/DELETE /api/contes-danses/**` (gestion de contes)
  * `POST/GET/PATCH /api/cartes-a-conte/**` (gestion des cartes)

---

## 📂 Modules Fonctionnels Implémentés

### 1. Utilisateurs & Authentification

Orchestration entre la base locale et Keycloak pour assurer l'intégrité des comptes.
* **Entité `Utilisateur`** :
  * Champs : `id` (UUID local), `keycloakId` (Sub JWT), `nom`, `prenom`, `email`, `role`, `dateNaissance`, `languePreferee`, `statut` (actif, inactif, suspendu), `ecole` (Relation ManyToOne).
* **Rôles disponibles (`RoleUtilisateur`)** : `eleve`, `enseignant`, `parent`, `professionnel_education`, `admin`, `comite_lecture`, `traducteur`, `ambassadeur`.
* **Fonctionnalités** :
  * Double inscription : créé dans Keycloak puis enregistré localement.
  * Récupération automatique du profil via `/me` grâce au token.
  * Administration complète des utilisateurs par rôle et modification de statut par l'admin.

---

### 2. Scolarité (Écoles, Classes & Inscriptions)

Modélisation du réseau des écoles et du système d'invitation dans les classes.
* **Entité `Ecole`** : `id` (UUID), `nom`, `adresse`, `codePostal`, `ville`.
* **Entité `Classe`** : `id` (UUID), `nom`, `niveau`, `codeInvitation` (code unique servant à l'inscription), `ecole` (ManyToOne).
* **Entité `InscriptionClasse`** : Table de liaison pour les élèves. `id` (UUID), `eleve` (Utilisateur, unique), `classe` (Classe), `dateInscription` (OffsetDateTime).

---

### 3. Actualités (`/api/actualites`)

Système de publication de nouvelles pour les administrateurs et consultation publique.
* **Entité `Actualite`** : `id` (UUID), `titre`, `contenu`, `resume`, `imageUrl`, `publie` (boolean), `datePublication`, `dateCreation`, `dateMiseAJour`, `auteur` (Utilisateur admin).
* **Fonctionnalités** :
  * Pagination et tri natifs (`Pageable`).
  * Filtrage et affichage exclusif des actualités publiées pour le public.
  * CRUD complet restreint aux administrateurs.

---

### 4. Contes Dansés (`/api/contes-danses`)

Catalogue de contes disponibles sous forme textuelle, audio ou vidéo.
* **Entité `ConteDanse`** :
  * Champs : `id` (UUID), `titre`, `description`, `thematique`, `langueOriginale`, `statut`, `acces`, `isbn`, URLs de fichiers (`fichierTexteUrl`, `fichierAudioUrl`, `fichierVideoUrl`), relations facultatives `ecole` / `classe`.
  * Enums associés :
    * `AccesConte` : `gratuit`, `payant`.
    * `StatutConte` : `brouillon`, `en_revision_enseignant`, `en_revision_comite`, `publie`, `refuse`.
* **Fonctionnalités** :
  * Consultation publique (paginée) des contes au statut `publie`.
  * Création ouverte à tout utilisateur authentifié.
  * Modification et suppression limitées à l'auteur du conte ou à l'administrateur.

---

### 5. Cartes à Conte (`/api/cartes-a-conte`)

Outils de création de cartes d'éléments pour concevoir ou illustrer des contes.
* **Entité `CarteAConte`** :
  * Champs : `id` (UUID), `createur` (Utilisateur), `type` (`personnage`, `lieu`, `objet_magique`), `imageUrl`, `texteAssocie`, `statutModeration` (`en_attente`, `valide`, `rejete`), `dateCreation`, relation `conte` (facultative).
* **Autorisation Fine (2 niveaux)** :
  * **Élève** : Peut uniquement **créer** une carte (son ID est récupéré du JWT) et **lister** ses propres cartes.
  * **Enseignant** : Peut **lister** les cartes créées par les élèves inscrits dans ses classes. Peut **valider** (PATCH `/{id}/valider`) uniquement ces mêmes cartes.
  * **Admin** : Accès global à toutes les cartes sans restriction.

---

### 6. Recherche Transversale (`/api/recherche`)

Endpoint unique permettant de chercher simultanément dans les bases d'**Actualités** et de **Contes Dansés**.
* **Endpoint** : `GET /api/recherche?q=terme-recherche`
* **Retour** : Un dictionnaire contenant les deux pages résultantes :
  ```json
  {
    "actualites": { ... },
    "contesDanses": { ... }
  }
  ```
* **Comportement** : Requêtes SQL performantes basées sur des requêtes JPA `LIKE` / `containing` insensibles à la casse (`ContainingIgnoreCase`).

---

## 🧪 Tests & Validation

La couverture de test est implémentée en JUnit / Spring Boot Test, intégrant notamment :
* `ActualiteServiceTest` : Tests unitaires et d'intégration validant le comportement du service et la pagination.
* `ConteDanseControllerTest` : Tests d'API (MockMvc) pour la sécurisation des endpoints de création, de modification et de suppression.
* `ProduitServiceTest` / `PanierServiceTest` / `CommandeServiceTest` : Tests unitaires des couches services de la boutique (gestion de stock, snapshotting, etc.).
* `CommandeControllerTest` : Tests d'intégration d'API (MockMvc) vérifiant la sécurité des endpoints de commande (utilisateurs authentifiés vs admins).
* `RessourcePedagogiqueServiceTest` : Tests unitaires du service Mallette Pédagogique (rôles, filtrage actif, exceptions).
* `RessourcePedagogiqueControllerTest` : Tests d'intégration d'API (MockMvc) pour la Mallette Pédagogique (droits par rôles, rejets sur inactif et interdiction de suppression).
* `ProjetConteServiceTest` : Tests unitaires du service Projets de conte (rattachement à l'enseignant, normalisation des ingrédients, contrôle du propriétaire, 404).
* `ProjetConteControllerTest` : Tests d'intégration d'API (MockMvc) pour les Projets de conte (401, 403 par rôle et par propriétaire, 400 de validation, 201).
* `ProjetConteRepositoryTest` : Tests d'intégration (H2) du mapping JPA des Projets de conte (ingrédients ordonnés, mise à jour, tri par date de modification).

---

### 7. Boutique & E-Commerce (Produits, Panier & Commandes)

Gestion complète du cycle de vente de produits physiques ou numériques.
* **Entités du module** :
  * `Produit` : `id` (UUID), `nom`, `description`, `prix`, `imageUrl`, `type` (`produit_physique`, `produit_numerique`), `stock` (nullable), `actif` (boolean).
  * `Panier` & `LignePanier` : Un panier par utilisateur. Contient des lignes de produits avec quantité. Clé unique sur `(panier_id, produit_id)`.
  * `AdresseLivraison` : Carnet d'adresses utilisateur (avec flag `parDefaut`).
  * `Commande` & `LigneCommande` : Enregistrement de la vente. Effectue un **snapshot du prix unitaire** au moment de l'achat et supporte la décrémentation des stocks des produits physiques.
* **Sécurité & Règles Métier** :
  * `GET /api/produits/**` : Public en lecture.
  * `POST/PUT/DELETE /api/produits/**` : Restreint aux administrateurs.
  * `/api/panier/**` : Sécurisé par JWT. Un utilisateur accède à son propre panier uniquement.
  * `/api/adresses-livraison/**` : Sécurisé par JWT. Modification autorisée uniquement pour le propriétaire de l'adresse.
  * `POST /api/commandes` : Sécurisé par JWT. Bloque la commande en cas de stock insuffisant, décrémente le stock, snapshot le prix du produit et vide le panier.
  * `PATCH /api/admin/commandes/{id}/statut` : Modification de l'état de la commande (ex: `en_attente`, `payee`, `expediee`, `annulee`) réservée à l'administrateur.

---

### 8. Mallette Pédagogique (`/api/ressources-pedagogiques`)

Bibliothèque de ressources pédagogiques (fiches, guides, vidéos, audio, documents) mise à disposition des enseignants.
* **Entités du module** :
  * `RessourcePedagogique` : `id` (UUID), `titre`, `description`, `type` (enum `TypeRessource` : `fiche`, `guide`, `video`, `audio`, `document`), `fichierUrl`, `thematique`, `niveauScolaire`, `dateAjout` (`OffsetDateTime`), `actif` (boolean).
* **Sécurité & Règles Métier** :
  * `GET /api/ressources-pedagogiques/**` : Accessible aux rôles `enseignant`, `professionnel_education`, `admin` et `comite_lecture`.
    * Les enseignants et professionnels de l'éducation accèdent uniquement aux ressources actives (`actif = true`). Les admins et comités de lecture accèdent à toutes les ressources (actives/inactives).
    * Ce filtrage de visibilité est validé au niveau requêtes paginées ET au niveau service par ID (renvoie `AccessDeniedException` si un enseignant demande une ressource inactive).
  * `POST/PUT /api/ressources-pedagogiques/**` : Réservé aux rôles `admin` et `comite_lecture`.
  * `DELETE /api/ressources-pedagogiques/{id}` : Réservé au rôle `admin` uniquement (`comite_lecture` n'a pas les droits de suppression).

---

### 9. Créer mon conte — Projets de conte (`/api/projets-contes`)

Projet de conte dansé d'un enseignant, construit pas à pas dans le parcours « Créer mon conte » (epic SCRUM-29). L'écran 1 (SCRUM-84 / SCRUM-96) renseigne le contexte du projet ; les écrans suivants enrichiront ce projet.
* **Entités du module** :
  * `ProjetConte` : `id` (UUID), `enseignant` (Utilisateur), `trancheAge` (`3-5`, `6-8`, `9-11`, `12-14`, `15-18`), `pays`, `region`, `ville` (facultative), `theme`, `ingredientsSecrets` (liste ordonnée de mots-clés, table `projets_contes_ingredients`), `espaceRepresentation` (enum `EspaceRepresentation` : `salle_spectacle`, `gymnase`, `classe`, `exterieur`, facultatif), `dateCreation`, `dateModification`.
* **Endpoints** :
  * `GET /api/projets-contes` : projets de l'enseignant connecté, du plus récemment modifié au plus ancien.
  * `GET /api/projets-contes/{id}` : consultation d'un projet.
  * `POST /api/projets-contes` : création d'un projet avec son contexte (201).
  * `PUT /api/projets-contes/{id}` : mise à jour du contexte.
* **Sécurité & Règles Métier** :
  * Rôles autorisés (`@PreAuthorize` sur le contrôleur) : `enseignant`, `professionnel_education`, `admin`.
  * Un utilisateur n'accède qu'à ses propres projets (`AccessDeniedException` → 403 sinon, 404 si le projet n'existe pas).
  * RG-CMC-01 : `trancheAge`, `pays`, `region` et `theme` obligatoires (400 avec le détail par champ).
  * RG-CMC-02 : ingrédients secrets libres, 20 maximum, 40 caractères chacun ; espaces retirés et doublons supprimés (insensible à la casse) en conservant l'ordre de saisie.


