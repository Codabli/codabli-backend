package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'un conte danse.
 * <p>
 * Le createur n'est PAS dans le DTO — il est extrait du JWT
 * dans le service (principe de securite).
 */
public record ConteDanseRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255)
        String titre,
        String description,
        @Size(max = 150)
        String thematique,
        @Size(max = 10)
        String langueOriginale,
        String couvertureUrl,
        @Size(max = 100)
        String pays,
        @Size(max = 150)
        String culture,
        Integer ageMin,
        Integer ageMax,
        Integer dureeMinutes,
        String credits,
        String fichierTexteUrl,
        String fichierAudioUrl,
        String fichierVideoUrl,
        UUID ecoleId,
        UUID classeId) {
}
