package com.codabli.repository;

import com.codabli.entity.RessourceFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RessourceFavoriteRepository extends JpaRepository<RessourceFavorite, UUID> {

    List<RessourceFavorite> findByUtilisateurId(UUID utilisateurId);

    Optional<RessourceFavorite> findByUtilisateurIdAndRessourceId(UUID utilisateurId, UUID ressourceId);
}
