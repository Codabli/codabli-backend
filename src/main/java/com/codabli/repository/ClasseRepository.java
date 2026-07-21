package com.codabli.repository;

import com.codabli.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClasseRepository extends JpaRepository<Classe, UUID> {

    List<Classe> findByEnseignantId(UUID enseignantId);
}
