package com.codabli.repository;

import com.codabli.entity.InscriptionClasse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour la table de liaison inscriptions_classes.
 *
 * La méthode clé ici est existsByEleveIdAndClasseEnseignantId().
 * Elle est utilisée par CarteAConteService.valider() pour vérifier
 * qu'un enseignant a bien le droit de valider la carte d'un élève.
 *
 * Spring Data dérive automatiquement la requête depuis le nom :
 * exists → retourne boolean (SELECT COUNT > 0)
 * ByEleve → champ "eleve" de InscriptionClasse (→ Utilisateur)
 * Id → champ "id" de l'Utilisateur
 * And → opérateur logique
 * Classe → champ "classe" de InscriptionClasse (→ Classe)
 * Enseignant → champ "enseignant" de Classe (→ Utilisateur)
 * Id → champ "id" de cet Utilisateur
 *
 * SQL généré (simplifié) :
 * SELECT COUNT(*) > 0 FROM inscriptions_classes ic
 * JOIN classes cl ON ic.classe_id = cl.id
 * WHERE ic.eleve_id = ?1 AND cl.enseignant_id = ?2
 */
@Repository
public interface InscriptionClasseRepository extends JpaRepository<InscriptionClasse, UUID> {

    /**
     * Vérifie si un élève est inscrit dans au moins une classe
     * dont l'enseignant donné est responsable.
     *
     * @param eleveId      ID de l'élève (créateur de la carte)
     * @param enseignantId ID de l'enseignant connecté
     * @return true si la relation existe, false sinon
     */
    boolean existsByEleveIdAndClasseEnseignantId(UUID eleveId, UUID enseignantId);
}
