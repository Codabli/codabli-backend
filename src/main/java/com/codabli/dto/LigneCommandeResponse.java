package com.codabli.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une ligne de commande.
 */
public record LigneCommandeResponse(
        UUID id,
        UUID produitId,
        String nomProduit,
        BigDecimal prixUnitaire,
        Integer quantite,
        BigDecimal sousTotal) {
}
