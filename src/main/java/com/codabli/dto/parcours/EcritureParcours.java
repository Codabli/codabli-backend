package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Ecran 7, ecriture : texte HTML de chaque etape de la trame, par identifiant
 * d'etape. Le titre est celui du conte (ConteDanse.titre).
 */
public record EcritureParcours(
        @NotNull Map<@NotBlank @Size(max = ID_MAX) String, @NotNull @Size(max = 50_000) String> texts)
        implements ContenuEtapeParcours {
}
