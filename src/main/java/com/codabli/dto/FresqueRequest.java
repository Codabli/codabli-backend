package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FresqueRequest(
        @NotNull(message = "La salle est obligatoire")
        UUID salleId,
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String imageUrl,
        String introduction,
        UUID conteId) {
}
