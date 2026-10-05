package com.codabli.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de requête pour ajouter une mise en avant dans la galerie.
 */
public record GalerieMiseEnAvantRequest(
        @NotNull(message = "L'identifiant de la carte est obligatoire")
        UUID carteAConteId,
        OffsetDateTime dateFin,
        @NotNull(message = "L'ordre d'affichage est obligatoire")
        Integer ordreAffichage) {
}
