package com.codabli.dto;

import com.codabli.entity.enums.TypeCibleMiseEnAvant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MiseEnAvantResponse(
        UUID id,
        TypeCibleMiseEnAvant typeCible,
        UUID cibleId,
        String titre,
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        int ordreAffichage,
        boolean actif) {
}
