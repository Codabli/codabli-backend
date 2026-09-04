# 📂 Modules Fonctionnels Implémentés — Codabli Backend

> Liste détaillée des modules du backend et de leurs règles d'accès.
> Pour l'installation et le lancement, voir le [README](README.md).

### 🔐 1. Sécurité & Utilisateurs
* Double inscription et synchronisation locale depuis le jeton **Keycloak** (JWT). Mappage des rôles du Realm : `eleve`, `enseignant`, `parent`, `professionnel_education`, `moderateur`, `admin`, `super_admin`, `comite_lecture`, `traducteur`, `ambassadeur`.
* Habilitation fine à 2 niveaux : `@PreAuthorize` au niveau contrôleur & validation logique métier spécifique dans les services.

### 🏫 2. Scolarité (Écoles, Classes & Inscriptions)
* Gestion des établissements scolaires (`Ecole`) et des classes (`Classe`) avec code d'invitation unique.
* Table d'inscription des élèves dans les classes.

### 📰 3. Actualités (`/api/actualites`)
* Flux d'actualités avec pagination et filtrage par statut de publication. CRUD complet restreint aux administrateurs.

### 📖 4. Contes Dansés (`/api/contes-danses`)
* Catalogue des contes avec pièces jointes (texte, audio et vidéo), couverture, pays, culture, tranche d'âge, durée et crédits. Filtres combinables (`langue`, `pays`, `thematique`, `acces`, `age`). Création par tout utilisateur connecté, modification restreinte à l'auteur ou à `admin`/`super_admin`/`moderateur`.
* **Workflow de publication** : `soumettre` (auteur) → `valider` par l'enseignant de la classe (`en_revision_enseignant` → `en_revision_comite`) → `valider` par le comité de lecture (→ `publie`) ; `admin`/`super_admin` publient directement. `refuser` disponible à chaque étape avec motif. `GET /mes-contes` et `GET /en-attente` pour suivre le cycle.
* **Multilingue** (`/api/contes-danses/{id}/traductions`) : chaque conte peut avoir plusieurs traductions suivant un cycle `a_traduire → en_cours → relecture → validee → publiee → archivee`, géré par `traducteur`/`comite_lecture`/`admin`/`super_admin`. Seules les traductions `publiee` sont visibles publiquement.

### 🎨 5. Cartes à Conte & Modération (`/api/cartes-a-conte`)
* Création d'éléments de contes (personnages, lieux, objets).
* **Flux de modération complet** (9 statuts CDC) : les élèves soumettent leurs cartes ; les enseignants valident, refusent ou demandent une correction uniquement pour les élèves de leurs propres classes ; `admin`/`super_admin`/`moderateur` ont un droit total, y compris le retrait d'une création déjà publiée.

### 📝 6. Fiches d'Activités (`/api/fiches-activites`)
* Fiches pédagogiques structurées (objectif, âge, durée, matériel, consignes, déroulement, compétences, adaptations, crédits, PDF). Mêmes règles de visibilité que la Mallette Pédagogique.

### 🔍 7. Recherche Transversale (`/api/recherche`)
* Endpoint unique permettant d'effectuer des recherches globales insensibles à la casse dans les actualités, les contes dansés, les cartes à conte validées, les produits et les partenaires actifs.

### 🛒 8. Boutique & E-Commerce (Produits, Panier & Commandes)
* **Produits** : Gestion des articles physiques et numériques (GET public, modification admin).
* **Panier** : Gestion persistée par utilisateur connecté avec calcul automatique des totaux.
* **Commande** : Passage de commande transactionnel avec :
  1. Décrémentation automatique des stocks physiques (déclenche `StockInsuffisantException` / 409 Conflict si stock insuffisant).
  2. Snapshotting (historisation) des prix unitaires et noms de produits pour conserver l'historique financier.
  3. Intégration d'adresses de livraison (gestion d'adresse par défaut).
  4. Calcul automatique de frais de port (5.00€ si présence d'au moins un article physique).
  5. Vidage de panier automatique après confirmation.

### 🎒 9. Mallette Pédagogique (`/api/ressources-pedagogiques`)
* Catalogue de matériel pédagogique (fiches, guides, vidéos, audio, documents) réservés aux enseignants.
* **Sécurité & Visibilité** :
  * `GET` pour `enseignant`, `professionnel_education`, `admin` et `comite_lecture`. Filtrage dynamique : les rôles hors-staff ne peuvent voir que les ressources actives (`actif = true`).
  * `POST/PUT` réservés aux rôles `admin` et `comite_lecture`.
  * `DELETE` réservé exclusivement aux administrateurs (`admin`).
* **Favoris** (RES-04) : `POST`/`DELETE .../{id}/favori` et `GET .../mes-favoris`.

### 🤝 10. Partenaires (`/api/partenaires`)
* Vitrine publique des partenaires (institutionnels, fondations, culturels, associations, citoyens contributeurs). `GET` public filtré sur `actif = true`, écriture réservée `admin`/`super_admin`.

### 💳 11. Abonnements (`/api/abonnements`)
* Catalogue d'offres (`/api/abonnements/offres`, public en lecture, géré par `admin`/`super_admin`) et souscriptions utilisateur : souscrire, renouveler, résilier, changer d'offre. Aucune passerelle de paiement réelle — la souscription active directement l'abonnement, comme pour les commandes de la boutique.

### 👶 12. Profils Enfants (`/api/profils-enfants`)
* Sous-profils légers rattachés au compte d'un `parent`/`professionnel_education` (pseudonyme, autorisation parentale datée et révocable), distincts du rôle `eleve` qui reste un compte Keycloak complet.

### ✉️ 13. Contact (`/api/contact`)
* Formulaire de contact public avec accusé de réception, catégorisation des demandes, et traitement par `admin`/`super_admin`.

### 🧾 14. Journal d'Activité (`/api/admin/journal-activite`)
* Historisation des actions sensibles (suppression/anonymisation/changement de statut d'un utilisateur, retrait d'une création publiée). Consultation réservée à `super_admin`.

### 📔 15. Carnet de Lecture & Carnet de Voyage (`/api/profils-enfants/{id}/carnet-lecture`, `.../carnet-voyage`)
* Espaces créatifs personnels de l'enfant : pages rattachées à un Conte Dansé (résumé, mots préférés, questions personnelles — carnet de lecture) ou à un pays/une fresque (identité, nature, société, culture, expérience — carnet de voyage), avec des éléments typés (personnages/lieux/objets/créations pour la lecture ; éléments naturels/culturels/personnages/objets/créations pour le voyage).
* Tout le contenu de texte libre est écrit par l'enfant : le backend ne génère jamais un résumé, une réponse ou une description à sa place (RG-10). Seules les métadonnées objectives du conte (titre, auteur, couverture, langue, pays) peuvent être préremplies.
* Confidentialité (RG-13) : accès réservé au responsable (`parent`/`professionnel_education`) propriétaire du profil enfant, ou à `admin`/`super_admin`.
* **Mallette d'Artistes** (`/api/profils-enfants/{id}/mallette-artistes`) : vue agrégée des deux carnets, des créations (dessins/collages/imports) et des pages prêtes à imprimer.

### 📊 16. Statistiques (`/api/admin/statistiques`)
* Tableau de bord agrégé (comptes par rôle, contes/cartes par statut, commandes, abonnements, pages de carnets, demandes de contact...). Réservé à `admin`/`super_admin`. Aucune donnée individuelle sur un enfant.

### 🎥 17. Webinaires (`/api/webinaires`)
* Catalogue public, inscription/désinscription pour tout utilisateur authentifié, `GET /mes-inscriptions`, limite de capacité optionnelle. Gestion réservée `admin`/`super_admin`.

### 🧑‍🏫 18. Coaching (`/api/coaching`)
* Catalogue d'offres public (`/offres`), réservation de créneau par tout utilisateur authentifié, `GET /reservations/mes-reservations`. Le propriétaire peut annuler sa réservation ; seul `admin`/`super_admin` peut la confirmer ou la clôturer.

### 📋 19. Évaluations Pédagogiques (`/api/evaluations-pedagogiques`)
* Grilles d'observation/compétences/bilan rédigées par un enseignant pour une classe et/ou un élève. Accès limité à l'auteur (ou `admin`/`super_admin`).

### 🌟 20. Mise en Avant (`/api/mise-en-avant`)
* Mécanisme générique de mise en avant sur la page d'accueil (actualité, conte, produit, abonnement, partenaire, événement) avec fenêtre de diffusion (`dateDebut`/`dateFin`) et ordre d'affichage. `GET` public ne retourne que les entrées actives et dans leur fenêtre. Gestion réservée `admin`/`super_admin`.

### 🖼️ 21. Galerie des Arts immersive (`/api/galerie-arts`)
* Socle structurel du musée culturel virtuel : salles par pays (hall), chacune avec sa fresque principale et ses hotspots (deux niveaux de lecture "Je découvre" 6-8 ans / "J'en apprends plus" 9-13 ans). `GET` public, gestion réservée `admin`/`super_admin`.
* **Hors périmètre de cette passe** : navigation immersive 2,5D (frontend), collecte d'autocollants et mini-jeux notés (gamification, traités séparément), soumission/modération des créations complémentaires d'une salle (GAL-23/24/25).

### 👤 Anonymisation utilisateur (`PATCH /api/admin/utilisateurs/{id}/anonymiser`)
* Alternative à la suppression définitive : révoque l'accès Keycloak et efface les données personnelles tout en conservant la ligne locale (intégrité référentielle avec les contenus créés par l'utilisateur).
