package com.codabli.dto.parcours;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import static com.codabli.dto.parcours.ContenuEtapeParcours.ID_MAX;

/**
 * Document ou lien de la classe (ecran 4). Pour un fichier, seules ses
 * informations sont enregistrees : le fichier reste sur l'appareil en
 * attendant le stockage S3. Les champs propres au fichier ou au lien sont
 * omis quand ils sont absents.
 *
 * @param addedAt         date d'ajout, ISO 8601
 * @param visibility      les creations importees sont privees : visibles du seul enseignant
 * @param size            taille du fichier en octets
 * @param rightsConfirmed droits d'utilisation pedagogiques attestes au televersement
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RessourceClasse(
        @NotBlank @Size(max = ID_MAX) String id,
        @NotNull TypeRessource kind,
        @NotNull @Size(max = 100) String name,
        @NotBlank @Size(max = 40) String addedAt,
        @NotNull @Pattern(regexp = "private") String visibility,
        @Size(max = ID_MAX) String fileId,
        @Size(max = 255) String fileName,
        @Size(max = 255) String mimeType,
        @PositiveOrZero Long size,
        Boolean rightsConfirmed,
        @Size(max = 2048) String url) {

    public enum TypeRessource { pdf, image, audio, video, link }
}
