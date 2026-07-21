package com.codabli.repository;

import com.codabli.entity.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, UUID> {
}
