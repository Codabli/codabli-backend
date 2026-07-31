package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une fiche d'activite (FIC-ACT-01/02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FicheActiviteResponse {

    private UUID id;

    private String titre;

    private String objectif;

    private Integer ageMin;

    private Integer ageMax;

    private Integer dureeMinutes;

    private String materiel;

    private String consignes;

    private String deroulement;

    private String competences;

    private String adaptations;

    private String credits;

    private String fichierPdfUrl;

    private boolean actif;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
