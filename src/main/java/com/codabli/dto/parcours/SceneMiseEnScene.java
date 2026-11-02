package com.codabli.dto.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mise en scene d'une scene (ecrans 8.1 a 8.4). Les danses et les sons sont
 * absents des brouillons enregistres avant les ecrans 8.2 et 8.4.
 *
 * @param props  decors et accessoires, en mots-cles
 * @param sounds bruitages et ambiance de la scene, hors danses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SceneMiseEnScene(
        @NotNull List<@NotNull @Valid PersonnageScene> characters,
        @NotNull @Size(max = 400) String intention,
        @NotNull List<@NotNull @Size(max = 40) String> props,
        List<@NotNull @Valid DanseScene> dances,
        List<@NotNull @Valid SonScene> sounds) {
}
