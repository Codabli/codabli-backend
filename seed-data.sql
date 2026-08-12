-- ============================================================================
-- Codabli — Script de données de test (seed)
-- ============================================================================
-- A EXECUTER APRES avoir lance l'application au moins une fois depuis IntelliJ
-- (ddl-auto: update cree/met a jour les tables au demarrage).
--
-- Ce script s'execute directement sur la base PostgreSQL "codabli_dev",
-- PAS depuis IntelliJ "Run" : utiliser l'onglet Database d'IntelliJ
-- (clic droit sur la connexion codabli_dev > New > Query Console, coller
-- ce fichier, executer), ou psql / DBeaver / pgAdmin.
--
-- IMPORTANT : les utilisateurs ci-dessous n'ont PAS de compte Keycloak reel
-- (keycloak_id = NULL). Ils servent uniquement a peupler les donnees
-- consultables via les endpoints GET (catalogue, galerie, etc.).
-- Pour tester une connexion reelle et les endpoints authentifies, il faut
-- creer un compte via POST /api/auth/register (qui cree Keycloak + la ligne
-- locale ensemble).
--
-- Le script est idempotent : relancer ne duplique rien (ON CONFLICT DO
-- NOTHING sur les cles primaires).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. ECOLES
-- ----------------------------------------------------------------------------
INSERT INTO ecoles (id, nom, pays, ville, niveau_abonnement, statut, date_creation, date_mise_a_jour) VALUES
('11111111-1111-1111-1111-100000000001', 'Ecole Primaire du Centre', 'France', 'Paris', 'pilote', 'actif', NOW(), NOW()),
('11111111-1111-1111-1111-100000000002', 'Ecole Internationale de Beyrouth', 'Liban', 'Beyrouth', 'pilote', 'actif', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. UTILISATEURS (un par role, pour tester chaque niveau d'acces)
-- ----------------------------------------------------------------------------
INSERT INTO utilisateurs (id, ecole_id, keycloak_id, nom, prenom, email, role, date_naissance, langue_preferee, statut, date_creation, date_mise_a_jour) VALUES
('22222222-2222-2222-2222-200000000001', NULL, NULL, 'Fontaine', 'Sophie', 'super.admin@codabli.test', 'super_admin', '1980-01-10', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000002', NULL, NULL, 'Bernard', 'Marc', 'admin@codabli.test', 'admin', '1982-03-22', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000003', NULL, NULL, 'Petit', 'Lucie', 'moderateur@codabli.test', 'moderateur', '1990-06-14', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000004', '11111111-1111-1111-1111-100000000001', NULL, 'Dupont', 'Jean', 'jean.dupont@codabli.test', 'enseignant', '1985-05-15', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000005', '11111111-1111-1111-1111-100000000001', NULL, 'Martin', 'Lea', 'lea.martin@codabli.test', 'eleve', '2016-09-01', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000006', '11111111-1111-1111-1111-100000000001', NULL, 'Durand', 'Noah', 'noah.durand@codabli.test', 'eleve', '2016-11-20', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000007', NULL, NULL, 'Curie', 'Marie', 'marie.curie@codabli.test', 'parent', '1979-12-02', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000008', NULL, NULL, 'Haddad', 'Nadia', 'nadia.haddad@codabli.test', 'professionnel_education', '1988-04-18', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000009', NULL, NULL, 'Rousseau', 'Paul', 'paul.rousseau@codabli.test', 'comite_lecture', '1975-07-30', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000010', NULL, NULL, 'Khalil', 'Rania', 'rania.khalil@codabli.test', 'traducteur', '1992-02-11', 'fr', 'actif', NOW(), NOW()),
('22222222-2222-2222-2222-200000000011', NULL, NULL, 'Moreau', 'Julien', 'julien.moreau@codabli.test', 'ambassadeur', '1983-08-09', 'fr', 'actif', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. CLASSES & INSCRIPTIONS
-- ----------------------------------------------------------------------------
INSERT INTO classes (id, ecole_id, enseignant_id, nom, niveau, annee_scolaire, date_creation) VALUES
('33333333-3333-3333-3333-300000000001', '11111111-1111-1111-1111-100000000001', '22222222-2222-2222-2222-200000000004', 'CM2-A', 'CM2', '2026-2027', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO inscriptions_classes (id, eleve_id, classe_id, date_inscription) VALUES
('44444444-4444-4444-4444-400000000001', '22222222-2222-2222-2222-200000000005', '33333333-3333-3333-3333-300000000001', NOW()),
('44444444-4444-4444-4444-400000000002', '22222222-2222-2222-2222-200000000006', '33333333-3333-3333-3333-300000000001', NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. ACTUALITES
-- ----------------------------------------------------------------------------
INSERT INTO actualites (id, auteur_id, titre, image_url, resume, contenu, publie, date_publication, date_creation, date_mise_a_jour) VALUES
('55555555-5555-5555-5555-500000000001', '22222222-2222-2222-2222-200000000002', 'Lancement de la plateforme Codabli', 'https://picsum.photos/seed/codabli1/800/400', 'La plateforme Codabli est desormais en ligne !', 'Nous sommes heureux de vous presenter la nouvelle plateforme numerique du Conte Danse.', true, NOW(), NOW(), NOW()),
('55555555-5555-5555-5555-500000000002', '22222222-2222-2222-2222-200000000002', 'Nouvelle Mallette Pedagogique disponible', 'https://picsum.photos/seed/codabli2/800/400', 'De nouvelles ressources pour les enseignants.', 'Plus de 20 fiches pedagogiques et fiches d''activites sont desormais disponibles.', true, NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. CONTES DANSES + TRADUCTIONS
-- ----------------------------------------------------------------------------
INSERT INTO contes_danses (id, ecole_id, classe_id, createur_id, titre, description, thematique, langue_originale,
    couverture_url, pays, culture, age_min, age_max, duree_minutes, credits,
    statut, acces, isbn, fichier_texte_url, fichier_audio_url, fichier_video_url, date_creation, date_publication) VALUES
('66666666-6666-6666-6666-600000000001', '11111111-1111-1111-1111-100000000001', '33333333-3333-3333-3333-300000000001',
    '22222222-2222-2222-2222-200000000004', 'La Danse des Etoiles', 'Un conte feerique sur une constellation egaree.', 'Astronomie',
    'fr', 'https://picsum.photos/seed/etoiles/600/800', 'France', 'Francophone', 6, 9, 12, 'Auteur : Hayat Harchi',
    'publie', 'gratuit', NULL, 'https://storage.example.com/contes/danse_etoiles.txt', 'https://storage.example.com/contes/danse_etoiles.mp3', NULL, NOW(), NOW()),
('66666666-6666-6666-6666-600000000002', NULL, NULL,
    '22222222-2222-2222-2222-200000000008', 'Le Voyage du Petit Cedre', 'Un jeune cedre part a la decouverte du Liban.', 'Nature & Voyage',
    'fr', 'https://picsum.photos/seed/cedre/600/800', 'Liban', 'Libanaise', 6, 10, 15, 'Auteur : Nadia Haddad',
    'publie', 'gratuit', NULL, 'https://storage.example.com/contes/petit_cedre.txt', NULL, NULL, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO contes_danses_traductions (id, conte_id, langue, variante, texte, audio_url, video_url, sous_titres_url,
    traducteur_id, relecteur_id, statut, date_creation, date_mise_a_jour) VALUES
('77777777-7777-7777-7777-700000000001', '66666666-6666-6666-6666-600000000002', 'en', NULL,
    'Once upon a time, a young cedar set out to discover Lebanon...', NULL, NULL, NULL,
    '22222222-2222-2222-2222-200000000010', NULL, 'publiee', NOW(), NOW()),
('77777777-7777-7777-7777-700000000002', '66666666-6666-6666-6666-600000000002', 'ar', NULL,
    NULL, NULL, NULL, NULL,
    '22222222-2222-2222-2222-200000000010', NULL, 'en_cours', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 6. CARTES A CONTE + GALERIE
-- ----------------------------------------------------------------------------
INSERT INTO cartes_a_conte (id, conte_id, createur_id, type, image_url, texte_associe, statut_moderation, motif_moderation, date_creation) VALUES
('88888888-8888-8888-8888-800000000001', '66666666-6666-6666-6666-600000000001', '22222222-2222-2222-2222-200000000005',
    'personnage', 'https://picsum.photos/seed/lutin/400/400', 'Un lutin malicieux qui cherche ses cles.', 'valide', NULL, NOW()),
('88888888-8888-8888-8888-800000000002', '66666666-6666-6666-6666-600000000001', '22222222-2222-2222-2222-200000000006',
    'lieu', 'https://picsum.photos/seed/foret/400/400', 'Une foret suspendue entre les etoiles.', 'soumis', NULL, NOW()),
('88888888-8888-8888-8888-800000000003', NULL, '22222222-2222-2222-2222-200000000005',
    'objet_magique', 'https://picsum.photos/seed/cle/400/400', 'Une cle qui ouvre toutes les portes.', 'a_corriger',
    'Merci de retirer le nom complet visible sur le dessin', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO galerie_mises_en_avant (id, carte_a_conte_id, date_debut, date_fin, ordre_affichage, actif, date_creation) VALUES
('99999999-9999-9999-9999-900000000001', '88888888-8888-8888-8888-800000000001', NOW(), NULL, 1, true, NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 7. RESSOURCES PEDAGOGIQUES & FICHES D'ACTIVITES
-- ----------------------------------------------------------------------------
INSERT INTO ressources_pedagogiques (id, titre, description, type, fichier_url, thematique, niveau_scolaire, date_ajout, actif) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-a00000000001', 'Fiche d''aide : La Danse des Oiseaux', 'Fiches d''exercices corporels associes au conte.', 'fiche',
    'https://storage.example.com/pedagogique/danse_oiseaux.pdf', 'Faune & Mouvement', 'CM1', NOW(), true),
('aaaaaaaa-aaaa-aaaa-aaaa-a00000000002', 'Guide de l''enseignant - Conte Danse', 'Guide methodologique complet.', 'guide',
    'https://storage.example.com/pedagogique/guide_enseignant.pdf', 'Methodologie', NULL, NOW(), true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fiches_activites (id, titre, objectif, age_min, age_max, duree_minutes, materiel, consignes, deroulement,
    competences, adaptations, credits, fichier_pdf_url, actif, date_creation, date_mise_a_jour) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-b00000000001', 'Danser les emotions du conte', 'Faire ressentir les emotions du recit par le mouvement',
    6, 9, 30, 'Un tapis de danse, une enceinte', 'Diviser la classe en petits groupes.', '1. Ecoute du conte - 2. Choix d''une emotion - 3. Improvisation',
    'Expression corporelle, ecoute active', 'Version assise possible pour les enfants a mobilite reduite', 'Concu par le comite de lecture Codabli',
    'https://storage.example.com/fiches/danser-emotions.pdf', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 8. BOUTIQUE : PRODUITS
-- ----------------------------------------------------------------------------
INSERT INTO produits (id, nom, description, prix, image_url, type, stock, actif, date_creation, date_mise_a_jour) VALUES
('cccccccc-cccc-cccc-cccc-c00000000001', 'Livre - La Danse des Etoiles', 'Edition imprimee du conte, illustree.', 14.90,
    'https://picsum.photos/seed/livre1/400/500', 'produit_physique', 25, true, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-c00000000002', 'Conte audio - Le Voyage du Petit Cedre', 'Version audio telechargeable, narration en francais.', 3.50,
    'https://picsum.photos/seed/audio1/400/500', 'produit_numerique', NULL, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 9. PARTENAIRES
-- ----------------------------------------------------------------------------
INSERT INTO partenaires (id, nom, logo_url, presentation, categorie, territoire, role_projet, video_url, lien,
    periode_debut, periode_fin, actif, date_creation, date_mise_a_jour) VALUES
('dddddddd-dddd-dddd-dddd-d00000000001', 'Institut Francais de Beyrouth', 'https://picsum.photos/seed/ifb/200/200',
    'Partenaire culturel pour la salle Liban de la Galerie des Arts.', 'culturel', 'Liban',
    'Fourniture de ressources culturelles et validation des contenus', NULL, 'https://institutfrancais-liban.com',
    '2026-01-01', NULL, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 10. ABONNEMENTS (offres + souscription d'exemple)
-- ----------------------------------------------------------------------------
INSERT INTO offres_abonnement (id, code, nom, description, public_cible, tarif, duree, limite_profils, limite_classes,
    stockage_mo, acces_webinaires, acces_coaching, acces_exports, actif, date_creation, date_mise_a_jour) VALUES
('eeeeeeee-eeee-eeee-eeee-e00000000001', 'famille_standard', 'Famille Standard', 'Acces complet a la Mallette d''artistes pour un enfant',
    'famille', 4.99, 'mensuel', 1, NULL, 500, false, false, true, true, NOW(), NOW()),
('eeeeeeee-eeee-eeee-eeee-e00000000002', 'enseignant_annuel', 'Enseignant Annuel', 'Acces complet a la Mallette Pedagogique et aux webinaires',
    'enseignant', 49.00, 'annuel', NULL, 3, 2000, true, true, true, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO abonnements (id, utilisateur_id, offre_id, statut, date_debut, date_fin, renouvellement_automatique, facture_url, date_creation) VALUES
('ffffffff-ffff-ffff-ffff-f00000000001', '22222222-2222-2222-2222-200000000007', 'eeeeeeee-eeee-eeee-eeee-e00000000001',
    'actif', NOW(), NOW() + INTERVAL '1 month', true, NULL, NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 11. PROFILS ENFANTS
-- ----------------------------------------------------------------------------
INSERT INTO profils_enfants (id, responsable_id, pseudonyme, date_naissance, langue_preferee, preferences, accessibilite,
    autorisation_parentale, date_autorisation, actif, date_creation, date_mise_a_jour) VALUES
('11111111-2222-3333-4444-100000000001', '22222222-2222-2222-2222-200000000007', 'Petit Lion', '2018-03-12', 'fr',
    'Aime les contes d''animaux', 'Grands caracteres recommandes', true, NOW(), true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 12. DEMANDES DE CONTACT
-- ----------------------------------------------------------------------------
INSERT INTO demandes_contact (id, utilisateur_id, categorie, nom, email, sujet, message, traite, date_creation) VALUES
('22222222-3333-4444-5555-200000000001', NULL, 'partenariat', 'Marie Curie', 'marie.curie@example.com',
    'Proposition de partenariat culturel', 'Bonjour, notre association souhaiterait collaborer avec Codabli.', false, NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 13. BOUTIQUE : ADRESSE, PANIER & COMMANDE
-- ----------------------------------------------------------------------------
INSERT INTO adresses_livraison (id, utilisateur_id, nom, adresse, code_postal, ville, telephone, par_defaut) VALUES
('33333333-4444-5555-6666-300000000001', '22222222-2222-2222-2222-200000000007', 'Domicile', '12 rue des Lilas', '75011', 'Paris', '0601020304', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO paniers (id, utilisateur_id, date_creation, date_mise_a_jour) VALUES
('44444444-5555-6666-7777-400000000001', '22222222-2222-2222-2222-200000000007', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO lignes_panier (id, panier_id, produit_id, quantite) VALUES
('55555555-6666-7777-8888-500000000001', '44444444-5555-6666-7777-400000000001', 'cccccccc-cccc-cccc-cccc-c00000000001', 2),
('55555555-6666-7777-8888-500000000002', '44444444-5555-6666-7777-400000000001', 'cccccccc-cccc-cccc-cccc-c00000000002', 1)
ON CONFLICT (id) DO NOTHING;

-- Commande deja finalisee (independante du panier ci-dessus), pour avoir un
-- exemple de LigneCommande avec snapshot prix/nom.
INSERT INTO commandes (id, utilisateur_id, statut, adresse_livraison_id, sous_total, frais_livraison, total, date_commande, reference_paiement) VALUES
('66666666-7777-8888-9999-600000000001', '22222222-2222-2222-2222-200000000007', 'payee', '33333333-4444-5555-6666-300000000001',
    18.40, 5.00, 23.40, NOW(), 'TEST-REF-0001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO lignes_commande (id, commande_id, produit_id, nom_produit, prix_unitaire, quantite) VALUES
('77777777-8888-9999-aaaa-700000000001', '66666666-7777-8888-9999-600000000001', 'cccccccc-cccc-cccc-cccc-c00000000001', 'Livre - La Danse des Etoiles', 14.90, 1),
('77777777-8888-9999-aaaa-700000000002', '66666666-7777-8888-9999-600000000001', 'cccccccc-cccc-cccc-cccc-c00000000002', 'Conte audio - Le Voyage du Petit Cedre', 3.50, 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Fin du script.
-- ============================================================================
