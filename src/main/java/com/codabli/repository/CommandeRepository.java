package com.codabli.repository;

import com.codabli.entity.Commande;
import com.codabli.entity.enums.StatutCommande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, UUID> {

    Page<Commande> findByUtilisateurIdOrderByDateCommandeDesc(UUID utilisateurId, Pageable pageable);

    long countByStatut(StatutCommande statut);
}
