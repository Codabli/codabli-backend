package com.codabli.repository;

import com.codabli.entity.Produit;
import com.codabli.entity.enums.TypeProduit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, UUID> {

    Page<Produit> findByActifTrue(Pageable pageable);

    Page<Produit> findByTypeAndActifTrue(TypeProduit type, Pageable pageable);

    Page<Produit> findByType(TypeProduit type, Pageable pageable);

    /**
     * Recherche par mot-cle (nom ou description) parmi les produits actifs.
     * Utilisee par la recherche transversale (REC-01).
     */
    @Query("""
            SELECT p FROM Produit p
            WHERE p.actif = true
            AND (LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Produit> rechercherActifs(@Param("q") String query, Pageable pageable);
}
