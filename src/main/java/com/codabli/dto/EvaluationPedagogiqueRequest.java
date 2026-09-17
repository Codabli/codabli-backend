package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une evaluation pedagogique
 * (PRO-05). L'enseignant n'est PAS dans le DTO — il est extrait du JWT.
 */
public record EvaluationPedagogiqueRequest(
        UUID classeId,
        UUID eleveId,
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String competencesEvaluees,
        String observations,
        String bilan,
        LocalDate dateEvaluation,
        String fichierExportUrl) {
}
