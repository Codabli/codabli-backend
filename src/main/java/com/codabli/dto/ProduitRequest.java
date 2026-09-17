package com.codabli.dto;

import com.codabli.entity.enums.TypeProduit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO d'entree pour la creation/modification d'un produit.
 * Reserve aux administrateurs.
 */
public record ProduitRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 255)
        String nom,
        String description,
        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix doit etre superieur a 0")
        BigDecimal prix,
        String imageUrl,
        @NotNull(message = "Le type est obligatoire")
        TypeProduit type,
        Integer stock,
        Boolean actif) {
}
