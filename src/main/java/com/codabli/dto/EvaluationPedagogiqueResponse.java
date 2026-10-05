package com.codabli.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EvaluationPedagogiqueResponse(
        UUID id,
        UUID enseignantId,
        String enseignantNom,
        UUID classeId,
        UUID eleveId,
        String eleveNom,
        String titre,
        String competencesEvaluees,
        String observations,
        String bilan,
        LocalDate dateEvaluation,
        String fichierExportUrl,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
