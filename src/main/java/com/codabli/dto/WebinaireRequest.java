package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record WebinaireRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String description,
        String intervenant,
        @NotNull(message = "La date de debut est obligatoire")
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        String lienUrl,
        String lienRediffusionUrl,
        String documentsUrl,
        Integer capaciteMax,
        Boolean actif) {
}
