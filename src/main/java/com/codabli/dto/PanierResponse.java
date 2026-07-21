package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour le panier complet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanierResponse {

    private UUID id;
    private List<LignePanierResponse> lignes;
    private BigDecimal total;
    private int nombreArticles;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateMiseAJour;
}
