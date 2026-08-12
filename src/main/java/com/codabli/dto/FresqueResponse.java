package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FresqueResponse {

    private UUID id;

    private UUID salleId;

    private String titre;

    private String imageUrl;

    private String introduction;

    private UUID conteId;

    private List<HotspotResponse> hotspots;
}
