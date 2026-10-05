package com.codabli.dto;

import java.util.UUID;

public record SalleGalerieResponse(
        UUID id,
        String pays,
        String introduction,
        String ambianceSonoreUrl,
        String paletteCouleurs,
        int ordreAffichage,
        boolean actif,
        //Null si aucune fresque n'a encore ete rattachee a la salle.
        FresqueResponse fresque) {
}
