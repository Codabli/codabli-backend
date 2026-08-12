package com.codabli.repository;

import com.codabli.entity.DemandeContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour DemandeContact (CDC 8.21).
 */
@Repository
public interface DemandeContactRepository extends JpaRepository<DemandeContact, UUID> {

    /**
     * Liste paginee des demandes, triees des plus recentes aux plus anciennes.
     * Reservee a l'administration.
     */
    Page<DemandeContact> findAllByOrderByDateCreationDesc(Pageable pageable);

    long countByTraite(boolean traite);
}
