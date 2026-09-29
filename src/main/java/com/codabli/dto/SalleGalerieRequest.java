package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record SalleGalerieRequest(
        @NotBlank(message = "Le pays est obligatoire")
        @Size(max = 100)
        String pays,
        String introduction,
        String ambianceSonoreUrl,
        String paletteCouleurs,
        Integer ordreAffichage,
        Boolean actif) {
}
