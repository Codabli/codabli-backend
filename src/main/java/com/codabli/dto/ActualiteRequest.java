package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour la creation/modification d'une actualite.
 *
 * Le champ "auteur" n'est PAS dans le DTO — il est extrait du JWT
 * dans le service (principe de securite).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualiteRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String titre;

    private String imageUrl;

    private String resume;

    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu;

    private Boolean publie;
}
