package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une page de carnet de
 * voyage (CDV-01 a 08). Champs optionnels : l'enfant avance a son rythme.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetVoyageRequest {

    /** Fourni uniquement a la creation, pour preremplir pays/drapeau/langues. */
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
}
