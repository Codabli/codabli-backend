package com.codabli.repository;

import com.codabli.entity.OffreAbonnement;
import com.codabli.entity.enums.PublicAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour OffreAbonnement (CDC 8.14, ABO-01).
 */
@Repository
public interface OffreAbonnementRepository extends JpaRepository<OffreAbonnement, UUID> {

    List<OffreAbonnement> findByActifTrue();

    List<OffreAbonnement> findByActifTrueAndPublicCible(PublicAbonnement publicCible);
}
