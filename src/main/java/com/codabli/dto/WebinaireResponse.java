package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebinaireResponse(
        UUID id,
        String titre,
        String description,
        String intervenant,
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        String lienUrl,
        String lienRediffusionUrl,
        String documentsUrl,
        Integer capaciteMax,
        long nombreInscrits,
        boolean actif,
        OffsetDateTime dateCreation) {
}
