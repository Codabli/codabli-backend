package com.codabli.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une page de carnet de
 * lecture (CDL-01/03/12/13). Les champs sont volontairement tous
 * optionnels : l'enfant avance a son rythme (RG-17, chemin non lineaire).
 */
public record PageCarnetLectureRequest(
        // Fourni uniquement a la creation, pour preremplir titre/auteur/couverture/langue.
        UUID conteId,
        String titre,
        String auteur,
        String couvertureUrl,
        String langue,
        LocalDate dateLecture,
        String theme,
        String resume,
        String motsPreferes,
        String questionsReponses) {
}
