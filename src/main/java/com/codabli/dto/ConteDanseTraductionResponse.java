package com.codabli.dto;

import com.codabli.entity.enums.StatutTraduction;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une traduction de Conte Danse (LAN-01).
 */
public record ConteDanseTraductionResponse(
        UUID id,
        UUID conteId,
        String langue,
        String variante,
        String texte,
        String audioUrl,
        String videoUrl,
        String sousTitresUrl,
        UUID traducteurId,
        String traducteurNom,
        UUID relecteurId,
        String relecteurNom,
        StatutTraduction statut,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
