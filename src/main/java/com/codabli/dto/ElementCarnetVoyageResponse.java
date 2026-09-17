package com.codabli.dto;

import com.codabli.entity.enums.TypeElementCarnetVoyage;

import java.util.UUID;

public record ElementCarnetVoyageResponse(
        UUID id,
        TypeElementCarnetVoyage type,
        String nom,
        String description,
        String imageUrl,
        int ordreAffichage) {
}
