package com.codabli.dto;

import java.util.UUID;

/**
 * Une creation (dessin/collage/import) retrouvee dans un carnet, exposee
 * dans la Mallette d'Artistes (8.16, "Mes dessins et collages").
 */
public record CreationMalletteResponse(
        //"carnet_lecture" ou "carnet_voyage"
        String source,
        UUID pageId,
        UUID elementId,
        String nom,
        String imageUrl) {
}
