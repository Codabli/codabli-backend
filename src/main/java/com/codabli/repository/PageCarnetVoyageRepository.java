package com.codabli.repository;

import com.codabli.entity.PageCarnetVoyage;
import com.codabli.entity.enums.StatutPageCarnet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour PageCarnetVoyage (CDC 8.17.2, CDV-14).
 */
@Repository
public interface PageCarnetVoyageRepository extends JpaRepository<PageCarnetVoyage, UUID> {

    List<PageCarnetVoyage> findByProfilEnfantIdOrderByDateCreationDesc(UUID profilEnfantId);

    long countByStatut(StatutPageCarnet statut);
}
