package com.codabli.dto.parcours;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Musique ou bruitage d'une scene (ecran 8.4). Le fichier reste sur
 * l'appareil en attendant le stockage S3 : seuls son identifiant et son nom
 * sont enregistres.
 *
 * @param danceId danse de la scene que le son accompagne, facultative
 */
public record SonScene(
        @NotBlank @Size(max = ID_MAX) String id,
        @NotNull @Size(max = 80) String title,
        @NotNull TypeSon kind,
        MomentScene moment,
        @Size(max = ID_MAX) String danceId,
        @NotBlank @Size(max = ID_MAX) String fileId,
        @NotNull @Size(max = 255) String fileName) {

    public enum TypeSon { music, effect }
}
