package com.codabli.repository;

import com.codabli.entity.JournalActivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour JournalActivite (CDC 8.23, SUP-03).
 */
@Repository
public interface JournalActiviteRepository extends JpaRepository<JournalActivite, UUID> {

    Page<JournalActivite> findAllByOrderByDateActionDesc(Pageable pageable);
}
