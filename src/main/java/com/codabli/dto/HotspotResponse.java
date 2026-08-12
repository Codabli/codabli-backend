package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotspotResponse {

    private UUID id;

    private UUID fresqueId;

    private String titre;

    private String explicationDecouverte;

    private String explicationApprofondie;

    private String imageUrl;

    private String audioUrl;

    private int ordreAffichage;

    private boolean actif;
}
