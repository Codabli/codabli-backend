package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotspotRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String explicationDecouverte,
        String explicationApprofondie,
        String imageUrl,
        String audioUrl,
        Integer ordreAffichage,
        Boolean actif) {
}
