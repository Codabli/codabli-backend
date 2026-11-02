package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Ecran 2, decouverte artistique : identifiants des ressources du catalogue
 * ajoutees au projet, realisees (RG-CMC-04) ou mises en favoris.
 */
public record DecouverteParcours(
        @NotNull List<@NotBlank @Size(max = ID_MAX) String> added,
        @NotNull List<@NotBlank @Size(max = ID_MAX) String> done,
        @NotNull List<@NotBlank @Size(max = ID_MAX) String> favorites) implements ContenuEtapeParcours {
}
