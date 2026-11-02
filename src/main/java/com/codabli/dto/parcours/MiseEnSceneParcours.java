package com.codabli.dto.parcours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/** Ecran 8, mise en scene : une scene par identifiant d'etape de la trame. */
public record MiseEnSceneParcours(
        @NotNull Map<@NotBlank @Size(max = ID_MAX) String, @NotNull @Valid SceneMiseEnScene> scenes)
        implements ContenuEtapeParcours {
}
