package com.codabli.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour modifier la quantite d'une ligne du panier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifQuantiteRequest {

    @NotNull(message = "La quantite est obligatoire")
    @Min(value = 1, message = "La quantite doit etre au moins 1")
    private Integer quantite;
}
