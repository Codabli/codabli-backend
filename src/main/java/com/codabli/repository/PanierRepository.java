package com.codabli.repository;

import com.codabli.entity.Panier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PanierRepository extends JpaRepository<Panier, UUID> {

    Optional<Panier> findByUtilisateurId(UUID utilisateurId);
}
