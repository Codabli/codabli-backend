package com.codabli.dto;

import com.codabli.entity.enums.TypeRessource;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une ressource.
 */
public record RessourcePedagogiqueResponse(
        UUID id,
        String titre,
        String description,
        TypeRessource type,
        String fichierUrl,
        String thematique,
        String niveauScolaire,
        OffsetDateTime dateAjout,
        boolean actif) {
}
