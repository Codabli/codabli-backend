package com.codabli.dto;

import com.codabli.entity.enums.StatutPageCarnet;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour une page de carnet de lecture (CDL-14, previsualisation).
 */
public record PageCarnetLectureResponse(
        UUID id,
        UUID profilEnfantId,
        UUID conteId,
        String titre,
        String auteur,
        String couvertureUrl,
        String langue,
        LocalDate dateLecture,
        String theme,
        String resume,
        String motsPreferes,
        String questionsReponses,
        StatutPageCarnet statut,
        String fichierExportUrl,
        List<ElementCarnetLectureResponse> elements,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
