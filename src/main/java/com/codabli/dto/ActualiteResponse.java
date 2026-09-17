package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une actualite.
 * Inclut les champs "auteurNom" et "auteurPrenom" pour le frontend.
 */
public record ActualiteResponse(
        UUID id,
        String titre,
        String imageUrl,
        String resume,
        String contenu,
        boolean publie,
        OffsetDateTime datePublication,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour,
        UUID auteurId,
        String auteurNom,
        String auteurPrenom) {
}
