package com.codabli.dto;

import com.codabli.entity.enums.TypeRessource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une ressource.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RessourcePedagogiqueResponse {

    private UUID id;
    private String titre;
    private String description;
    private TypeRessource type;
    private String fichierUrl;
    private String thematique;
    private String niveauScolaire;
    private OffsetDateTime dateAjout;
    private boolean actif;
}
