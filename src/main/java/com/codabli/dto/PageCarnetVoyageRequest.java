package com.codabli.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une page de carnet de
 * voyage (CDV-01 a 08). Champs optionnels : l'enfant avance a son rythme.
 */
public record PageCarnetVoyageRequest(
        // Fourni uniquement a la creation, pour preremplir pays/drapeau/langues.
        UUID conteId,
        String pays,
        String drapeauUrl,
        String languesDecouvertes,
        LocalDate dateVisite,
        String identiteNotes,
        String natureNotes,
        String societeNotes,
        String cultureNotes,
        String experienceNotes) {
}
