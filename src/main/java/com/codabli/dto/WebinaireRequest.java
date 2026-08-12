package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebinaireRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String titre;

    private String description;

    private String intervenant;

    @NotNull(message = "La date de debut est obligatoire")
    private OffsetDateTime dateDebut;

    private OffsetDateTime dateFin;

    private String lienUrl;

    private String lienRediffusionUrl;

    private String documentsUrl;

    private Integer capaciteMax;

    private Boolean actif;
}
