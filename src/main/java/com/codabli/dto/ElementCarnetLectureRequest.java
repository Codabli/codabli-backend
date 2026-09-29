package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetLecture;
import jakarta.validation.constraints.NotNull;

/**
 * DTO d'entree pour ajouter un element (personnage/lieu/objet/creation) a
 * une page de carnet de lecture (CDL-04/05/06/09/11).
 */
public record ElementCarnetLectureRequest(
        @NotNull(message = "Le type d'element est obligatoire")
        TypeElementCarnetLecture type,
        String nom,
        String description,
        String imageUrl,
        Integer ordreAffichage) {
}
