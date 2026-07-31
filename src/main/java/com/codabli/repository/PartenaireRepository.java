package com.codabli.repository;

import com.codabli.entity.Partenaire;
import com.codabli.entity.enums.CategoriePartenaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour Partenaire (CDC 8.13, PAR-01/02).
 */
@Repository
public interface PartenaireRepository extends JpaRepository<Partenaire, UUID> {

    /**
     * Liste paginee des partenaires actifs, avec filtre optionnel par categorie.
     */
    Page<Partenaire> findByActifTrueAndCategorie(CategoriePartenaire categorie, Pageable pageable);

    /**
     * Liste paginee de tous les partenaires actifs (sans filtre de categorie).
     */
    Page<Partenaire> findByActifTrue(Pageable pageable);

    /**
     * Recherche par mot-cle (nom ou presentation) parmi les partenaires actifs.
     * Utilisee par la recherche transversale (REC-01).
     */
    @Query("""
            SELECT p FROM Partenaire p
            WHERE p.actif = true
            AND (LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(p.presentation) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Partenaire> rechercherActifs(@Param("q") String query, Pageable pageable);
}
