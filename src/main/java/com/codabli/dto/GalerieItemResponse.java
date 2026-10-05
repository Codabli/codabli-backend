package com.codabli.dto;

import com.codabli.entity.enums.TypeCarte;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un élément de la galerie publique.
 * N'expose pas le statut de modération (toujours valide dans la galerie).
 */
public record GalerieItemResponse(
        UUID id,
        TypeCarte type,
        String imageUrl,
        String texteAssocie,
        OffsetDateTime dateCreation,
        String createurPrenom,
        boolean miseEnAvant) {
}
