package com.codabli.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour le panier complet.
 */
public record PanierResponse(
        UUID id,
        List<LignePanierResponse> lignes,
        BigDecimal total,
        int nombreArticles,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
