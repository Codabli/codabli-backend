package com.codabli.dto.parcours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Ecran 4, ressources de la classe : documents deposes et liens externes. */
public record RessourcesClasseParcours(
        @NotNull List<@NotNull @Valid RessourceClasse> items) implements ContenuEtapeParcours {
}
