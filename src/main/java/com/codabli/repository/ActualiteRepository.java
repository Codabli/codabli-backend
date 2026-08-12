package com.codabli.repository;

import com.codabli.entity.Actualite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour Actualite.
 *
 * Deux niveaux d'acces :
 * 1. PUBLIC : actualites publiees uniquement (findByPublieTrue)
 * 2. ADMIN : toutes les actualites (findAll herite de JpaRepository)
 */
@Repository
public interface ActualiteRepository extends JpaRepository<Actualite, UUID> {

    /**
     * Liste paginee des actualites publiees, triees par date de publication
     * descendante.
     */
    Page<Actualite> findByPublieTrueOrderByDatePublicationDesc(Pageable pageable);

    long countByPublieTrue();

    /**
     * Recherche dans les actualites publiees par mot-cle (titre ou resume).
     */
    @Query("""
            SELECT a FROM Actualite a
            WHERE a.publie = true
            AND (LOWER(a.titre) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(a.resume) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.datePublication DESC
            """)
    Page<Actualite> rechercherPubliees(@Param("q") String query, Pageable pageable);
}
