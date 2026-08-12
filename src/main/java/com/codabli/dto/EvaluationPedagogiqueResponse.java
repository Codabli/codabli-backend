package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationPedagogiqueResponse {

    private UUID id;

    private UUID enseignantId;

    private String enseignantNom;

    private UUID classeId;

    private UUID eleveId;

    private String eleveNom;

    private String titre;

    private String competencesEvaluees;

    private String observations;

    private String bilan;

    private LocalDate dateEvaluation;

    private String fichierExportUrl;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
