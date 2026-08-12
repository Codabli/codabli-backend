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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetVoyageResponse {

    private UUID id;

    private UUID profilEnfantId;

    private UUID conteId;

    private String pays;

    private String drapeauUrl;

    private String languesDecouvertes;

    private LocalDate dateVisite;

    private String identiteNotes;

    private String natureNotes;

    private String societeNotes;

    private String cultureNotes;

    private String experienceNotes;

    private StatutPageCarnet statut;

    private String fichierExportUrl;

    private List<ElementCarnetVoyageResponse> elements;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
