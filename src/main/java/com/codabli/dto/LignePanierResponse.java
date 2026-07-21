package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une ligne du panier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LignePanierResponse {

    private UUID id;
    private UUID produitId;
    private String produitNom;
    private String produitImageUrl;
    private BigDecimal produitPrix;
    private Integer quantite;
    private BigDecimal sousTotal;
}
