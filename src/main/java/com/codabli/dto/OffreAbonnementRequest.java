package com.codabli.dto;

import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO d'entree pour la creation/modification d'une offre d'abonnement
 * (ABO-01).
 */
public record OffreAbonnementRequest(
        @NotBlank(message = "Le code est obligatoire")
        @Size(max = 100)
        String code,
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 255)
        String nom,
        String description,
        @NotNull(message = "Le public cible est obligatoire")
        PublicAbonnement publicCible,
        @NotNull(message = "Le tarif est obligatoire")
        @Positive(message = "Le tarif doit etre positif")
        BigDecimal tarif,
        @NotNull(message = "La duree est obligatoire")
        DureeAbonnement duree,
        Integer limiteProfils,
        Integer limiteClasses,
        Integer stockageMo,
        Boolean accesWebinaires,
        Boolean accesCoaching,
        Boolean accesExports,
        Boolean actif) {
}
