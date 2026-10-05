package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une entree du journal d'activite (SUP-03).
 */
public record JournalActiviteResponse(
        UUID id,
        UUID acteurId,
        String acteurNom,
        String action,
        String cible,
        String details,
        OffsetDateTime dateAction) {
}
