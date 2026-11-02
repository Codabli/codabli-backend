package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Etape de la trame (ecran 6). Une etape proposee par defaut (RG-CMC-09) a un
 * type ; une etape ajoutee par la classe n'en a pas.
 *
 * @param characterIds personnages de l'univers presents dans l'etape
 */
public record EtapeTrame(
        @NotBlank @Size(max = ID_MAX) String id,
        TypeEtape kind,
        @NotNull @Size(max = 60) String title,
        @NotNull @Size(max = 400) String summary,
        @NotNull List<@NotBlank @Size(max = ID_MAX) String> characterIds) {

    public enum TypeEtape { initialSituation, trigger, adventures, resolution, finalSituation }
}
