package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une mise en avant (vue admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalerieMiseEnAvantResponse {

    private UUID id;
    private UUID carteAConteId;
    private String titreCarte;
    private OffsetDateTime dateDebut;
    private OffsetDateTime dateFin;
    private Integer ordreAffichage;
    private boolean actif;
}
