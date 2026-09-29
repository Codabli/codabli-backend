package com.codabli.dto;

import java.util.List;
import java.util.UUID;

public record FresqueResponse(
        UUID id,
        UUID salleId,
        String titre,
        String imageUrl,
        String introduction,
        UUID conteId,
        List<HotspotResponse> hotspots) {
}
