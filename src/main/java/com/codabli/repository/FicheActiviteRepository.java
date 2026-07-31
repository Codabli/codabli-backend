package com.codabli.repository;

import com.codabli.entity.FicheActivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository pour FicheActivite (CDC 8.7, FIC-ACT-01/02).
 */
public interface FicheActiviteRepository extends JpaRepository<FicheActivite, UUID> {

    @Query("""
            SELECT f FROM FicheActivite f
            WHERE (:actif IS NULL OR f.actif = :actif)
            AND (:age IS NULL OR (
                (f.ageMin IS NULL OR f.ageMin <= :age)
                AND (f.ageMax IS NULL OR f.ageMax >= :age)
            ))
            """)
    Page<FicheActivite> findByFilters(@Param("actif") Boolean actif, @Param("age") Integer age, Pageable pageable);
}
