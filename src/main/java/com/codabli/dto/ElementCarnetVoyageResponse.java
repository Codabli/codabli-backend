package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetVoyage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetVoyageResponse {

    private UUID id;

    private TypeElementCarnetVoyage type;

    private String nom;

    private String description;

    private String imageUrl;

    private int ordreAffichage;
}
