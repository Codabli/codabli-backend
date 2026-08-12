package com.codabli.dto;

import com.codabli.entity.enums.StatutPageCarnet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour une page de carnet de lecture (CDL-14, previsualisation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetLectureResponse {

    private UUID id;

    private UUID profilEnfantId;

    private UUID conteId;

    private String titre;

    private String auteur;

    private String couvertureUrl;

    private String langue;

    private LocalDate dateLecture;

    private String theme;

    private String resume;

    private String motsPreferes;

    private String questionsReponses;

    private StatutPageCarnet statut;

    private String fichierExportUrl;

    private List<ElementCarnetLectureResponse> elements;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
