package com.codabli.dto;

import com.codabli.entity.enums.TypeRessource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la création et modification d'une ressource.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RessourcePedagogiqueRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150, message = "Le titre ne peut pas depasser 150 caracteres")
    private String titre;

    private String description;

    @NotNull(message = "Le type de ressource est obligatoire")
    private TypeRessource type;

    private String fichierUrl;

    private String thematique;

    private String niveauScolaire;

    private Boolean actif;
}
