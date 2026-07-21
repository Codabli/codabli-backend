package com.codabli.repository;

import com.codabli.entity.Produit;
import com.codabli.entity.enums.TypeProduit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, UUID> {

    Page<Produit> findByActifTrue(Pageable pageable);

    Page<Produit> findByTypeAndActifTrue(TypeProduit type, Pageable pageable);

    Page<Produit> findByType(TypeProduit type, Pageable pageable);
}
