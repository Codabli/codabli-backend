package com.codabli.dto.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Personnage d'une scene : sur scene des le debut ou entrant en cours de
 * scene, restant jusqu'a la fin ou sortant. Le moment precis de l'entree ou
 * de la sortie est en texte libre, omis s'il est absent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonnageScene(
        @NotBlank @Size(max = ID_MAX) String characterId,
        @NotNull Entree entrance,
        @Size(max = 150) String entranceMoment,
        @NotNull Sortie exit,
        @Size(max = 150) String exitMoment) {

    public enum Entree { onStage, enters }

    public enum Sortie { stays, exits }
}
