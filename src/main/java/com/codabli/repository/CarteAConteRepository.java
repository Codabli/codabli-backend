package com.codabli.repository;

import com.codabli.entity.CarteAConte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codabli.entity.enums.StatutModeration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour CarteAConte avec des requêtes filtrées selon le rôle.
 *
 * Trois niveaux d'accès aux données :
 *
 * 1. ÉLÈVE → findByCreateurId()
 * Dérivé automatiquement par Spring Data grâce à la convention de nommage :
 * "findBy" + "Createur" (nom du champ @ManyToOne) + "Id" (champ id de
 * Utilisateur).
 * Spring génère : SELECT c FROM CarteAConte c WHERE c.createur.id = ?1
 *
 * 2. ENSEIGNANT → findByEnseignantClasses()
 * Requête JPQL personnalisée avec sous-requête.
 * Chemin de jointure :
 * CarteAConte.createur ←→ InscriptionClasse.eleve
 * InscriptionClasse.classe ←→ Classe.enseignant
 * Résultat : toutes les cartes dont le créateur est inscrit dans
 * une classe dont l'enseignant connecté est responsable.
 *
 * 3. ADMIN → findAll() (hérité de JpaRepository, pas besoin de le redéfinir)
 */
@Repository
public interface CarteAConteRepository extends JpaRepository<CarteAConte, UUID> {

    /**
     * Cartes créées par un élève précis.
     * Utilisé quand le token JWT a le rôle "eleve".
     */
    List<CarteAConte> findByCreateurId(UUID createurId);

    /**
     * Cartes des élèves inscrits dans les classes de l'enseignant donné.
     *
     * La sous-requête sélectionne tous les IDs d'élèves (ic.eleve.id)
     * qui sont inscrits (InscriptionClasse) dans une classe (ic.classe)
     * dont l'enseignant (ic.classe.enseignant.id) correspond à l'ID fourni.
     *
     * La requête principale filtre ensuite les CarteAConte dont le
     * créateur fait partie de cette liste d'élèves.
     */
    @Query("""
            SELECT c FROM CarteAConte c
            WHERE c.createur.id IN (
                SELECT ic.eleve.id FROM InscriptionClasse ic
                WHERE ic.classe.enseignant.id = :enseignantId
            )
            """)
    List<CarteAConte> findByEnseignantClasses(@Param("enseignantId") UUID enseignantId);

    /**
     * Trouve une carte par ID et statut de modération.
     * Utilisé par la galerie publique pour ne retourner que les cartes validées.
     */
    Optional<CarteAConte> findByIdAndStatutModeration(UUID id, StatutModeration statutModeration);

    /**
     * Recherche par mot-cle (texte associe) parmi les cartes validées.
     * Utilisée par la recherche transversale (REC-01), mêmes règles de
     * visibilité que la galerie publique.
     */
    @Query("""
            SELECT c FROM CarteAConte c
            WHERE c.statutModeration = :statut
            AND LOWER(c.texteAssocie) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<CarteAConte> rechercherValidees(
            @Param("statut") StatutModeration statut,
            @Param("q") String query,
            Pageable pageable);

    long countByStatutModeration(StatutModeration statut);
}
