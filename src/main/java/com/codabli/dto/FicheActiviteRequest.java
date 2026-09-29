package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour la creation/modification d'une fiche d'activite
 * (FIC-ACT-01).
 */
public record FicheActiviteRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String objectif,
        Integer ageMin,
        Integer ageMax,
        Integer dureeMinutes,
        String materiel,
        String consignes,
        String deroulement,
        String competences,
        String adaptations,
        String credits,
        String fichierPdfUrl,
        Boolean actif) {
}
