package com.codabli.dto;

import com.codabli.entity.enums.TypeProduit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un produit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitResponse {

    private UUID id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private String imageUrl;
    private TypeProduit type;
    private Integer stock;
    private boolean actif;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateMiseAJour;
}
