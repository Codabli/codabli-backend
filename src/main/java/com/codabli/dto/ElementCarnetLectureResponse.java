package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetLecture;

import java.util.UUID;

public record ElementCarnetLectureResponse(
        UUID id,
        TypeElementCarnetLecture type,
        String nom,
        String description,
        String imageUrl,
        int ordreAffichage) {
}
