package com.codabli.dto;

import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une offre d'abonnement (ABO-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreAbonnementResponse {

    private UUID id;

    private String code;

    private String nom;

    private String description;

    private PublicAbonnement publicCible;

    private BigDecimal tarif;

    private DureeAbonnement duree;

    private Integer limiteProfils;

    private Integer limiteClasses;

    private Integer stockageMo;

    private boolean accesWebinaires;

    private boolean accesCoaching;

    private boolean accesExports;

    private boolean actif;
}
