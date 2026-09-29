package com.codabli.dto;

import com.codabli.entity.enums.StatutPageCarnet;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PageCarnetVoyageResponse(
        UUID id,
        UUID profilEnfantId,
        UUID conteId,
        String pays,
        String drapeauUrl,
        String languesDecouvertes,
        LocalDate dateVisite,
        String identiteNotes,
        String natureNotes,
        String societeNotes,
        String cultureNotes,
        String experienceNotes,
        StatutPageCarnet statut,
        String fichierExportUrl,
        List<ElementCarnetVoyageResponse> elements,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
