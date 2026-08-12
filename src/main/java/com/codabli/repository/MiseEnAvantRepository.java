package com.codabli.repository;

import com.codabli.entity.MiseEnAvant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MiseEnAvantRepository extends JpaRepository<MiseEnAvant, UUID> {

    /**
     * Mises en avant actives et dans leur fenetre de diffusion (ACC-04).
     */
    @Query("""
            SELECT m FROM MiseEnAvant m
            WHERE m.actif = true
            AND m.dateDebut <= :maintenant
            AND (m.dateFin IS NULL OR m.dateFin >= :maintenant)
            ORDER BY m.ordreAffichage ASC
            """)
    List<MiseEnAvant> findActivesEnCours(@Param("maintenant") OffsetDateTime maintenant);
}
