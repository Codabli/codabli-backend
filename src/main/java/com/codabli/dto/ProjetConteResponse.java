package com.codabli.dto;

import com.codabli.entity.enums.EspaceRepresentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProjetConteResponse(
        UUID id,
        UUID enseignantId,
        String trancheAge,
        String pays,
        String region,
        String ville,
        String theme,
        List<String> ingredientsSecrets,
        EspaceRepresentation espaceRepresentation,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification) {
}
