package com.codabli.dto.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Danse de la bibliotheque placee dans une scene (ecran 8.2).
 *
 * @param danceId    identifiant de la danse dans la bibliotheque
 * @param placements placement des personnages et decors pour cette danse (ecran 8.3)
 * @param sounds     ambiance musicale de la danse (ecran 8.4)
 */
public record DanseScene(
        @NotBlank @Size(max = ID_MAX) String id,
        @NotBlank @Size(max = ID_MAX) String danceId,
        @NotNull @Size(max = 60) String name,
        MomentScene moment,
        @NotNull Style style,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<@NotNull @Valid PlacementScene> placements,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<@NotNull @Valid SonScene> sounds) {

    public enum Style { traditional, classical, contemporary }
}
