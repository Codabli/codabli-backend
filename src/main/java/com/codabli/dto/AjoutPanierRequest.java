package com.codabli.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO d'entree pour ajouter un produit au panier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjoutPanierRequest {

    @NotNull(message = "L'identifiant du produit est obligatoire")
    private UUID produitId;

    @NotNull(message = "La quantite est obligatoire")
    @Min(value = 1, message = "La quantite doit etre au moins 1")
    private Integer quantite;
}
