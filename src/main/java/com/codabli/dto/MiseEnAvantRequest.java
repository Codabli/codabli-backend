package com.codabli.dto;

import com.codabli.entity.enums.TypeCibleMiseEnAvant;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MiseEnAvantRequest(
        @NotNull(message = "Le type de cible est obligatoire")
        TypeCibleMiseEnAvant typeCible,
        @NotNull(message = "L'identifiant de la cible est obligatoire")
        UUID cibleId,
        String titre,
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        Integer ordreAffichage,
        Boolean actif) {
}
