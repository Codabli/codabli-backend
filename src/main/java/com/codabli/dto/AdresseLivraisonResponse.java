package com.codabli.dto;

import java.util.UUID;

/**
 * DTO de sortie pour une adresse de livraison.
 */
public record AdresseLivraisonResponse(
        UUID id,
        String nom,
        String adresse,
        String codePostal,
        String ville,
        String telephone,
        boolean parDefaut
) {
}
