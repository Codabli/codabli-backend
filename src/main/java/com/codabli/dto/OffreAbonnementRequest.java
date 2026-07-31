package com.codabli.dto;

import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO d'entree pour la creation/modification d'une offre d'abonnement
 * (ABO-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreAbonnementRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 100)
    private String code;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255)
    private String nom;

    private String description;

    @NotNull(message = "Le public cible est obligatoire")
    private PublicAbonnement publicCible;

    @NotNull(message = "Le tarif est obligatoire")
    @Positive(message = "Le tarif doit etre positif")
    private BigDecimal tarif;

    @NotNull(message = "La duree est obligatoire")
    private DureeAbonnement duree;

    private Integer limiteProfils;

    private Integer limiteClasses;

    private Integer stockageMo;

    private Boolean accesWebinaires;

    private Boolean accesCoaching;

    private Boolean accesExports;

    private Boolean actif;
}
