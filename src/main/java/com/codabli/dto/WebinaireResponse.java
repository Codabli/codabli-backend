package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebinaireResponse {

    private UUID id;

    private String titre;

    private String description;

    private String intervenant;

    private OffsetDateTime dateDebut;

    private OffsetDateTime dateFin;

    private String lienUrl;

    private String lienRediffusionUrl;

    private String documentsUrl;

    private Integer capaciteMax;

    private long nombreInscrits;

    private boolean actif;

    private OffsetDateTime dateCreation;
}
