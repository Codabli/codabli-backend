package com.codabli.dto;

import com.codabli.entity.enums.CategorieContact;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une demande de contact.
 * Le champ "accuseReception" porte le message de confirmation d'envoi
 * (CNT-02).
 */
public record ContactResponse(
        UUID id,
        CategorieContact categorie,
        String nom,
        String email,
        String sujet,
        String message,
        boolean traite,
        OffsetDateTime dateCreation,
        String accuseReception) {
}
