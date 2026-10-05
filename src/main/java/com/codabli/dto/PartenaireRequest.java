package com.codabli.dto;

import com.codabli.entity.enums.CategoriePartenaire;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO d'entree pour la creation/modification d'un partenaire (PAR-01/02).
 */
public record PartenaireRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 255)
        String nom,
        String logoUrl,
        String presentation,
        @NotNull(message = "La categorie est obligatoire")
        CategoriePartenaire categorie,
        String territoire,
        String roleProjet,
        String videoUrl,
        String lien,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        Boolean actif) {
}
