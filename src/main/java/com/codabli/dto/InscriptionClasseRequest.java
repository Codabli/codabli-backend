package com.codabli.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InscriptionClasseRequest(
        @NotNull(message = "L'ID de l'eleve est obligatoire")
        UUID eleveId,
        @NotNull(message = "L'ID de la classe est obligatoire")
        UUID classeId) {
}
