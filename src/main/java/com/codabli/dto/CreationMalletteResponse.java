package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Une creation (dessin/collage/import) retrouvee dans un carnet, exposee
 * dans la Mallette d'Artistes (8.16, "Mes dessins et collages").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationMalletteResponse {

    /** "carnet_lecture" ou "carnet_voyage" */
    private String source;

    private UUID pageId;

    private UUID elementId;

    private String nom;

    private String imageUrl;
}
