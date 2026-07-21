package com.codabli.repository;

import com.codabli.entity.AdresseLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdresseLivraisonRepository extends JpaRepository<AdresseLivraison, UUID> {

    List<AdresseLivraison> findByUtilisateurId(UUID utilisateurId);
}
