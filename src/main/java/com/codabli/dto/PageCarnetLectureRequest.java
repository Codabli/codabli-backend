package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une page de carnet de
 * lecture (CDL-01/03/12/13). Les champs sont volontairement tous
 * optionnels : l'enfant avance a son rythme (RG-17, chemin non lineaire).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetLectureRequest {

    /** Fourni uniquement a la creation, pour preremplir titre/auteur/couverture/langue. */
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
}
