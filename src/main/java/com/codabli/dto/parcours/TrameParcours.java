package com.codabli.dto.parcours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Ecran 6, trame du recit : ses etapes, dans l'ordre. */
public record TrameParcours(
        @NotNull List<@NotNull @Valid EtapeTrame> steps) implements ContenuEtapeParcours {
}
