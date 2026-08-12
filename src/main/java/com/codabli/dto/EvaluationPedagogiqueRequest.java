package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une evaluation pedagogique
 * (PRO-05). L'enseignant n'est PAS dans le DTO — il est extrait du JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationPedagogiqueRequest {

    private UUID classeId;

    private UUID eleveId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String titre;

    private String competencesEvaluees;

    private String observations;

    private String bilan;

    private LocalDate dateEvaluation;

    private String fichierExportUrl;
}
