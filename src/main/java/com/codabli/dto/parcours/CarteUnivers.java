package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Lieu, epoque ou objet de l'univers (ecran 5). L'image reste sur l'appareil
 * en attendant le stockage S3 : seul son identifiant est enregistre.
 */
public record CarteUnivers(
        @NotBlank @Size(max = ID_MAX) String id,
        @NotNull @Size(max = 60) String name,
        @NotNull @Size(max = 500) String description,
        @Size(max = ID_MAX) String imageId) {
}
