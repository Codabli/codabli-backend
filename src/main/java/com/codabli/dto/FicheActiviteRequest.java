package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour la creation/modification d'une fiche d'activite
 * (FIC-ACT-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FicheActiviteRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
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

    private Boolean actif;
}
