package com.codabli.repository;

import com.codabli.entity.Abonnement;
import com.codabli.entity.enums.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour Abonnement (CDC 8.14, ABO-02/03/04).
 */
@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, UUID> {

    List<Abonnement> findByUtilisateurIdOrderByDateCreationDesc(UUID utilisateurId);

    long countByStatut(StatutAbonnement statut);
}
