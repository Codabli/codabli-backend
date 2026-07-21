package com.codabli.repository;

import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByKeycloakId(String keycloakId);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByRole(RoleUtilisateur role);

    List<Utilisateur> findByStatut(String statut);
}
