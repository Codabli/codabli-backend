package com.codabli.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO d'entree pour ajouter un produit au panier.
 */
public record AjoutPanierRequest(
        @NotNull(message = "L'identifiant du produit est obligatoire")
        UUID produitId,
        @NotNull(message = "La quantite est obligatoire")
        @Min(value = 1, message = "La quantite doit etre au moins 1")
        Integer quantite){
}
