package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/** Personnage de l'univers (ecran 5) : RG-CMC-08, un role, une description et un objectif. */
public record PersonnageUnivers(
        @NotBlank @Size(max = ID_MAX) String id,
        @NotNull @Size(max = 60) String name,
        @NotNull @Size(max = 500) String description,
        @Size(max = ID_MAX) String imageId,
        @NotNull @Size(max = 40) String role,
        @NotNull @Size(max = 500) String goal) {
}
