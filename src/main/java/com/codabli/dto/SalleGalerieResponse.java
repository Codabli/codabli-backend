package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleGalerieResponse {

    private UUID id;

    private String pays;

    private String introduction;

    private String ambianceSonoreUrl;

    private String paletteCouleurs;

    private int ordreAffichage;

    private boolean actif;

    /** Null si aucune fresque n'a encore ete rattachee a la salle. */
    private FresqueResponse fresque;
}
