package com.codabli.dto.parcours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Ecran 5, univers du conte : lieux, personnages, epoques et objets. */
public record UniversParcours(
        @NotNull List<@NotNull @Valid CarteUnivers> places,
        @NotNull List<@NotNull @Valid PersonnageUnivers> characters,
        @NotNull List<@NotNull @Valid CarteUnivers> periods,
        @NotNull List<@NotNull @Valid CarteUnivers> objects) implements ContenuEtapeParcours {
}
