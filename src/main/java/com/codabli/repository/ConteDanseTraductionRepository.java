package com.codabli.repository;

import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.enums.StatutTraduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour ConteDanseTraduction (CDC 8.5, LAN-01/02/03).
 */
@Repository
public interface ConteDanseTraductionRepository extends JpaRepository<ConteDanseTraduction, UUID> {

    /**
     * Traductions publiees d'un conte (visibles publiquement, LAN-01/03).
     */
    List<ConteDanseTraduction> findByConteIdAndStatut(UUID conteId, StatutTraduction statut);

    /**
     * Toutes les traductions d'un conte, quel que soit leur statut (gestion
     * traducteur/comite de lecture/admin, LAN-02).
     */
    List<ConteDanseTraduction> findByConteId(UUID conteId);
}
