package com.codabli.dto;

import com.codabli.entity.enums.StatutAbonnement;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une souscription (ABO-03/04).
 */

public record AbonnementResponse(
        UUID id,
        UUID offreId,
        String offreNom,
        String offreCode,
        StatutAbonnement statut,
        OffsetDateTime dateDebut,
        OffsetDateTime dateFin,
        Boolean renouvellementAutomatique,
        String factureUrl,
        OffsetDateTime dateCreation) {

}
