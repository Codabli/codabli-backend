package com.codabli.dto;

import com.codabli.entity.enums.TypeCarte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un élément de la galerie publique.
 * N'expose pas le statut de modération (toujours valide dans la galerie).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalerieItemResponse {

    private UUID id;
    private TypeCarte type;
    private String imageUrl;
    private String texteAssocie;
    private OffsetDateTime dateCreation;
    private String createurPrenom;
    private boolean miseEnAvant;
}
