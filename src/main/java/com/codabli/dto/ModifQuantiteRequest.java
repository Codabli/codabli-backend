package com.codabli.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO d'entree pour modifier la quantite d'une ligne du panier.
 */

public record ModifQuantiteRequest(
        @NotNull(message = "La quantite est obligatoire")
        @Min(value = 1, message = "La quantite doit etre au moins 1")
        Integer quantite) {
}
