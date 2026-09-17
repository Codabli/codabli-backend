package com.codabli.dto;

import java.util.UUID;

public record HotspotResponse(
        UUID id,
        UUID fresqueId,
        String titre,
        String explicationDecouverte,
        String explicationApprofondie,
        String imageUrl,
        String audioUrl,
        int ordreAffichage,
        boolean actif) {
}
