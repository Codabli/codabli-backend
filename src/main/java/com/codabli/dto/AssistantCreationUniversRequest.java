package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'aide à la réflexion sur un élément de l'univers d'un conte.
 */
public record AssistantCreationUniversRequest(
        @NotNull(message = "Le type est obligatoire")
        Type type,
        @NotBlank(message = "La description est obligatoire")
        String description) {

    public enum Type {
        Lieu,
        Personnage,
        Epoque,
        Objet
    }
}
