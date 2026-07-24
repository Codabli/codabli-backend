package com.codabli.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de requête pour ajouter une mise en avant dans la galerie.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalerieMiseEnAvantRequest {

    @NotNull(message = "L'identifiant de la carte est obligatoire")
    private UUID carteAConteId;

    private OffsetDateTime dateFin;

    @NotNull(message = "L'ordre d'affichage est obligatoire")
    private Integer ordreAffichage;
}
