package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetVoyage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetVoyageRequest {

    @NotNull(message = "Le type d'element est obligatoire")
    private TypeElementCarnetVoyage type;

    private String nom;

    private String description;

    private String imageUrl;

    private Integer ordreAffichage;
}
