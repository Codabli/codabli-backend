package com.codabli.repository;

import com.codabli.entity.ConteDanse;
import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour ConteDanse.
 *
 * Trois niveaux d'acces :
 * 1. PUBLIC : contes publies uniquement (findByStatut publie)
 * 2. AUTEUR : ses propres contes (findByCreateurId)
 * 3. ADMIN : tous les contes (findAll herite de JpaRepository)
 */
@Repository
public interface ConteDanseRepository extends JpaRepository<ConteDanse, UUID> {

    /**
     * Liste paginee des contes publies, tries par date de publication descendante.
     */
    Page<ConteDanse> findByStatutOrderByDatePublicationDesc(StatutConte statut, Pageable pageable);

    /**
     * Contes crees par un utilisateur precis.
     */
    List<ConteDanse> findByCreateurId(UUID createurId);

    /**
     * Recherche dans les contes publies par mot-cle (titre ou description).
     */
    @Query("""
            SELECT c FROM ConteDanse c
            WHERE c.statut = :statut
            AND (LOWER(c.titre) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY c.datePublication DESC
            """)
    Page<ConteDanse> rechercherPublies(@Param("statut") StatutConte statut,
            @Param("q") String query,
            Pageable pageable);

    /**
     * Filtre les contes publies (CON-03) : age, langue, pays, thematique et
     * niveau d'acces. Chaque parametre nul est ignore (filtre non applique).
     */
    @Query("""
            SELECT c FROM ConteDanse c
            WHERE c.statut = :statut
            AND (:langue IS NULL OR c.langueOriginale = :langue)
            AND (:pays IS NULL OR LOWER(c.pays) = LOWER(:pays))
            AND (:thematique IS NULL OR LOWER(c.thematique) LIKE LOWER(CONCAT('%', :thematique, '%')))
            AND (:acces IS NULL OR c.acces = :acces)
            AND (:age IS NULL OR (
                (c.ageMin IS NULL OR c.ageMin <= :age)
                AND (c.ageMax IS NULL OR c.ageMax >= :age)
            ))
            ORDER BY c.datePublication DESC
            """)
    Page<ConteDanse> filtrerPublies(@Param("statut") StatutConte statut,
            @Param("langue") String langue,
            @Param("pays") String pays,
            @Param("thematique") String thematique,
            @Param("acces") AccesConte acces,
            @Param("age") Integer age,
            Pageable pageable);
}
