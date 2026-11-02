package com.codabli.repository;

import com.codabli.entity.ParcoursConte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParcoursConteRepository extends JpaRepository<ParcoursConte, UUID> {

    Optional<ParcoursConte> findByConteDanseId(UUID conteDanseId);
}
