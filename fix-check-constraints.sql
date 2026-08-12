-- ============================================================================
-- Correctif : contraintes CHECK obsoletes generees par Hibernate sur les
-- colonnes d'enum (role, statut_moderation).
--
-- Hibernate cree automatiquement une contrainte CHECK listant les valeurs
-- d'un enum au moment de la CREATION de la table. Avec ddl-auto: update,
-- cette contrainte n'est PAS mise a jour quand on ajoute de nouvelles
-- valeurs a l'enum cote Java (ex: moderateur/super_admin, ou les nouveaux
-- statuts de moderation). Elle bloque alors les nouvelles valeurs.
--
-- A executer une seule fois dans pgAdmin (Query Tool sur codabli_dev).
-- ============================================================================

-- 1) Diagnostic (optionnel) : liste les contraintes CHECK actuelles sur ces
--    deux tables, pour verifier les noms avant de les supprimer.
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'c'
  AND conrelid IN ('utilisateurs'::regclass, 'cartes_a_conte'::regclass)
ORDER BY table_name, conname;

-- 2) Suppression des contraintes obsoletes sur les colonnes d'enum.
--    (Le nom exact est confirme par le message d'erreur / la requete ci-dessus.
--    Si le nom differe pour cartes_a_conte, adapter d'apres le resultat du
--    diagnostic.)
ALTER TABLE utilisateurs DROP CONSTRAINT IF EXISTS utilisateurs_role_check;
ALTER TABLE cartes_a_conte DROP CONSTRAINT IF EXISTS cartes_a_conte_statut_moderation_check;

-- 3) Verification : la liste ci-dessus (etape 1) ne doit plus montrer ces
--    deux contraintes une fois relancee.

-- Remarque : on ne recree PAS de CHECK avec la nouvelle liste de valeurs.
-- L'application valide deja les enums cote Java (Jackson/Hibernate rejette
-- toute valeur hors enum avant meme d'atteindre la base), et sans Flyway/
-- Liquibase dans ce projet, une contrainte recreee ici redeviendrait
-- obsolete a la prochaine evolution d'un enum. Si vous preferez malgre tout
-- une securite au niveau base, decommentez et executez :
--
-- ALTER TABLE utilisateurs ADD CONSTRAINT utilisateurs_role_check
--   CHECK (role IN ('eleve','enseignant','parent','professionnel_education',
--                    'moderateur','admin','super_admin','comite_lecture',
--                    'traducteur','ambassadeur'));
--
-- ALTER TABLE cartes_a_conte ADD CONSTRAINT cartes_a_conte_statut_moderation_check
--   CHECK (statut_moderation IN ('brouillon','soumis','en_verification',
--                                 'a_corriger','valide','refuse','publie',
--                                 'retire','archive'));
