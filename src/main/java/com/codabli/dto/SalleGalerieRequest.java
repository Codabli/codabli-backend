package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleGalerieRequest {

    @NotBlank(message = "Le pays est obligatoire")
    @Size(max = 100)
    private String pays;

    private String introduction;

    private String ambianceSonoreUrl;

    private String paletteCouleurs;

    private Integer ordreAffichage;

    private Boolean actif;
}
