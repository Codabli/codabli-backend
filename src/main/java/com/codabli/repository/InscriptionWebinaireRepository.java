package com.codabli.repository;

import com.codabli.entity.InscriptionWebinaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InscriptionWebinaireRepository extends JpaRepository<InscriptionWebinaire, UUID> {

    List<InscriptionWebinaire> findByUtilisateurIdOrderByDateInscriptionDesc(UUID utilisateurId);

    Optional<InscriptionWebinaire> findByWebinaireIdAndUtilisateurId(UUID webinaireId, UUID utilisateurId);

    long countByWebinaireId(UUID webinaireId);
}
