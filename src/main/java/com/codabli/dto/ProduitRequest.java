package com.codabli.dto;

import com.codabli.entity.enums.TypeProduit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO d'entree pour la creation/modification d'un produit.
 * Reserve aux administrateurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255)
    private String nom;

    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit etre superieur a 0")
    private BigDecimal prix;

    private String imageUrl;

    @NotNull(message = "Le type est obligatoire")
    private TypeProduit type;

    private Integer stock;

    private Boolean actif;
}
