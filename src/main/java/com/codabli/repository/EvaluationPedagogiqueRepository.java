package com.codabli.repository;

import com.codabli.entity.EvaluationPedagogique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationPedagogiqueRepository extends JpaRepository<EvaluationPedagogique, UUID> {

    List<EvaluationPedagogique> findByEnseignantIdOrderByDateEvaluationDesc(UUID enseignantId);

    List<EvaluationPedagogique> findByClasseIdOrderByDateEvaluationDesc(UUID classeId);
}
