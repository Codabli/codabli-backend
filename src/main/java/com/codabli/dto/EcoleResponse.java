package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EcoleResponse(
        UUID id,
        String nom,
        String pays,
        String ville,
        String niveauAbonnement,
        String statut,
        OffsetDateTime dateCreation) {
}
