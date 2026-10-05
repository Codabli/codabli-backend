package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OffreCoachingRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String description,
        Integer dureeMinutes,
        @NotNull(message = "Le tarif est obligatoire")
        @Positive(message = "Le tarif doit etre positif")
        BigDecimal tarif,
        Boolean actif) {
}
