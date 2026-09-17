package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClasseRequest(
        @NotNull(message = "L'ID de l'ecole est obligatoire")
        UUID ecoleId,
        @NotNull(message = "L'ID de l'enseignant est obligatoire")
        UUID enseignantId,
        @NotBlank(message = "Le nom de la classe est obligatoire")
        @Size(max = 150)
        String nom,
        @Size(max = 50)
        String niveau,
        @NotBlank(message = "L'annee scolaire est obligatoire")
        @Size(max = 20)
        String anneeScolaire) {
}
