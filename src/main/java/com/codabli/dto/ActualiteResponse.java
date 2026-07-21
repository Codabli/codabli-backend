package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une actualite.
 * Inclut les champs "auteurNom" et "auteurPrenom" pour le frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualiteResponse {

    private UUID id;

    private String titre;

    private String imageUrl;

    private String resume;

    private String contenu;

    private boolean publie;

    private OffsetDateTime datePublication;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;

    /** ID de l'auteur (admin) */
    private UUID auteurId;

    /** Nom de l'auteur */
    private String auteurNom;

    /** Prenom de l'auteur */
    private String auteurPrenom;
}
