package com.codabli.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une ligne du panier.
 */
public record LignePanierResponse(
        UUID id,
        UUID produitId,
        String produitNom,
        String produitImageUrl,
        BigDecimal produitPrix,
        Integer quantite,
        BigDecimal sousTotal) {
}
