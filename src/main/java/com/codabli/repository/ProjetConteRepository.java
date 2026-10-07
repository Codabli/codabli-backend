package com.codabli.repository;

import com.codabli.entity.ProjetConte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjetConteRepository extends JpaRepository<ProjetConte, UUID> {

    List<ProjetConte> findByEnseignantIdOrderByDateModificationDesc(UUID enseignantId);
}
