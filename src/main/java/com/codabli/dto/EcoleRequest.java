package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EcoleRequest(
        @NotBlank(message = "Le nom de l'ecole est obligatoire")
        @Size(max = 200)
        String nom,
        @NotBlank(message = "Le pays est obligatoire")
        @Size(max = 100)
        String pays,
        @Size(max = 100)
        String ville) {
}
