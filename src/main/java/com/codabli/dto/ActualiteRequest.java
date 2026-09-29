package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour la creation/modification d'une actualite.
 * <p>
 * Le champ "auteur" n'est PAS dans le DTO — il est extrait du JWT
 * dans le service (principe de securite).
 */

public record ActualiteRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String imageUrl,
        String resume,
        @NotBlank(message = "Le contenu est obligatoire")
        String contenu,
        Boolean publie) {
}
