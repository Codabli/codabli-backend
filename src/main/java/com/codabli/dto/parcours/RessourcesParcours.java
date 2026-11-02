package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Ecran 3, ressources culturelles : identifiants des ressources ajoutees au
 * corpus du projet.
 */
public record RessourcesParcours(
        @NotNull List<@NotBlank @Size(max = ID_MAX) String> added) implements ContenuEtapeParcours {
}
