package com.codabli.dto.parcours;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Position d'un personnage ou d'un decor sur le plateau au debut de la danse,
 * en pourcentage (x : de gauche a droite vu du public, y : du fond de scene
 * vers le public).
 *
 * @param ref identifiant du personnage, ou libelle du decor
 */
public record PlacementScene(
        @NotNull TypeElement kind,
        @NotBlank @Size(max = ID_MAX) String ref,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double x,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double y) {

    public enum TypeElement { character, prop }
}
