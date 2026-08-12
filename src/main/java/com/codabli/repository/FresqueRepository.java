package com.codabli.repository;

import com.codabli.entity.Fresque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FresqueRepository extends JpaRepository<Fresque, UUID> {

    Optional<Fresque> findBySalleId(UUID salleId);
}
