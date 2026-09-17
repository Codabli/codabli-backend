package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une fiche d'activite (FIC-ACT-01/02).
 */
public record FicheActiviteResponse(
        UUID id,
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
        boolean actif,
        OffsetDateTime dateCreation,
        OffsetDateTime dateMiseAJour) {
}
