package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetLecture;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour ajouter un element (personnage/lieu/objet/creation) a
 * une page de carnet de lecture (CDL-04/05/06/09/11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetLectureRequest {

    @NotNull(message = "Le type d'element est obligatoire")
    private TypeElementCarnetLecture type;

    private String nom;

    private String description;

    private String imageUrl;

    private Integer ordreAffichage;
}
