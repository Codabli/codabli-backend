package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetVoyage;
import jakarta.validation.constraints.NotNull;

public record ElementCarnetVoyageRequest(
        @NotNull(message = "Le type d'element est obligatoire")
        TypeElementCarnetVoyage type,
        String nom,
        String description,
        String imageUrl,
        Integer ordreAffichage) {
}
