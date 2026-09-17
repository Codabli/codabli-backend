package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une mise en avant (vue admin).
 */
public record GalerieMiseEnAvantResponse(
        UUID id,
        UUID carteAConteId,
        String titreCarte,
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        Integer ordreAffichage,
        boolean actif) {

}
