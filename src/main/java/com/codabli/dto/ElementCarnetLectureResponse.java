package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetLecture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetLectureResponse {

    private UUID id;

    private TypeElementCarnetLecture type;

    private String nom;

    private String description;

    private String imageUrl;

    private int ordreAffichage;
}
