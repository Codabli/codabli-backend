package com.codabli.dto;

import com.codabli.entity.enums.TypeRessource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requête pour la création et modification d'une ressource.
 */
public record RessourcePedagogiqueRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne peut pas depasser 150 caracteres")
        String titre,
        String description,
        @NotNull(message = "Le type de ressource est obligatoire")
        TypeRessource type,
        String fichierUrl,
        String thematique,
        String niveauScolaire,
        Boolean actif) {
}
