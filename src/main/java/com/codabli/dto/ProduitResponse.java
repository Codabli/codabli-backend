package com.codabli.dto;

import com.codabli.entity.enums.TypeProduit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un produit.
 */
public record ProduitResponse(
        UUID id,
        String nom,
        String description,
        BigDecimal prix,
        String imageUrl,
        TypeProduit type,
        Integer stock,
        boolean actif,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
