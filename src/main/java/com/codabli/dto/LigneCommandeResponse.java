package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de sortie pour une ligne de commande.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommandeResponse {

    private UUID id;
    private UUID produitId;
    private String nomProduit;
    private BigDecimal prixUnitaire;
    private Integer quantite;
    private BigDecimal sousTotal;
}
