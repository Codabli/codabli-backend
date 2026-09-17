package com.codabli.dto;

import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un conte danse.
 * Inclut les champs "createurNom" et "createurPrenom" pour le frontend.
 */
public record ConteDanseResponse(
        UUID id,
        String titre,
        String description,
        String thematique,
        String langueOriginale,
        String couvertureUrl,
        String pays,
        String culture,
        Integer ageMin,
        Integer ageMax,
        Integer dureeMinutes,
        String credits,
        StatutConte statut,
        AccesConte acces,
        String isbn,
        String fichierTexteUrl,
        String fichierAudioUrl,
        String fichierVideoUrl,
        String motifModeration,
        OffsetDateTime dateCreation,
        OffsetDateTime datePublication,
        UUID createurId,
        String createurNom,
        String createurPrenom,
        UUID ecoleId,
        UUID classeId) {
}
