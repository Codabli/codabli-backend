package com.codabli.dto;

import com.codabli.entity.enums.CategoriePartenaire;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un partenaire (PAR-02).
 */
public record PartenaireResponse(
        UUID id,
        String nom,
        String logoUrl,
        String presentation,
        CategoriePartenaire categorie,
        String territoire,
        String roleProjet,
        String videoUrl,
        String lien,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        boolean actif,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
