package com.codabli.repository;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.entity.GalerieMiseEnAvant;
import com.codabli.entity.enums.StatutModeration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour la galerie d'art.
 *
 * Requête principale : retourne les CarteAConte validées triées avec
 * les mises en avant actives en premier (par ordreAffichage),
 * puis le reste par dateCreation DESC.
 *
 * La règle « carte mise en avant redevenue non valide → disparaît
 * automatiquement » est gérée au niveau du WHERE JPQL
 * (statutModeration = valide), pas par un trigger ou cron.
 */
@Repository
public interface GalerieMiseEnAvantRepository extends JpaRepository<GalerieMiseEnAvant, UUID> {

    /**
     * Retourne les cartes validées pour la galerie publique.
     *
     * LEFT JOIN sur GalerieMiseEnAvant pour déterminer si la carte
     * est mise en avant. Les mises en avant actives apparaissent en
     * premier (triées par ordreAffichage), suivies des cartes non
     * mises en avant (triées par dateCreation DESC).
     *
     * Une mise en avant est considérée « active » si :
     * - actif = true
     * - dateDebut <= now
     * - dateFin IS NULL OU dateFin >= now
     * - la carte associée a statutModeration = valide
     */
    @Query("""
            SELECT new com.codabli.dto.GalerieItemResponse(
                c.id, c.type, c.imageUrl, c.texteAssocie, c.dateCreation,
                c.createur.prenom,
                CASE WHEN m.id IS NOT NULL AND m.actif = true
                          AND m.dateDebut <= :now
                          AND (m.dateFin IS NULL OR m.dateFin >= :now)
                     THEN true ELSE false END
            )
            FROM CarteAConte c
            LEFT JOIN GalerieMiseEnAvant m ON m.carteAConte = c
                AND m.actif = true
                AND m.dateDebut <= :now
                AND (m.dateFin IS NULL OR m.dateFin >= :now)
            WHERE c.statutModeration = :statut
            ORDER BY
                CASE WHEN m.id IS NOT NULL AND m.actif = true
                          AND m.dateDebut <= :now
                          AND (m.dateFin IS NULL OR m.dateFin >= :now)
                     THEN 0 ELSE 1 END ASC,
                COALESCE(m.ordreAffichage, 2147483647) ASC,
                c.dateCreation DESC
            """)
    Page<GalerieItemResponse> findCartesForGalerie(
            @Param("statut") StatutModeration statut,
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    /**
     * Liste des mises en avant actives (pour l'admin).
     */
    List<GalerieMiseEnAvant> findByActifTrue();
}
