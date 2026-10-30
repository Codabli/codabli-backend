package com.codabli.dto;

import com.codabli.entity.enums.EspaceRepresentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Etat du parcours "Creer mon conte danse" d'un conte. Une etape jamais
 * enregistree vaut null.
 */
public record ParcoursConteResponse(
        UUID conteDanseId,
        String region,
        String ville,
        EspaceRepresentation espaceRepresentation,
        List<String> ingredientsSecrets,
        Map<String, Object> decouverte,
        Map<String, Object> ressources,
        Map<String, Object> ressourcesClasse,
        Map<String, Object> univers,
        Map<String, Object> trame,
        Map<String, Object> ecriture,
        Map<String, Object> miseEnScene,
        OffsetDateTime dateModification) {
}
