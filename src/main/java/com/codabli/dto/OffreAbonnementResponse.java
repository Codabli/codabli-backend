package com.codabli.dto;

import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une offre d'abonnement (ABO-01).
 */
public record OffreAbonnementResponse(
        UUID id,
        String code,
        String nom,
        String description,
        PublicAbonnement publicCible,
        BigDecimal tarif,
        DureeAbonnement duree,
        Integer limiteProfils,
        Integer limiteClasses,
        Integer stockageMo,
        boolean accesWebinaires,
        boolean accesCoaching,
        boolean accesExports,
        boolean actif) {
}
