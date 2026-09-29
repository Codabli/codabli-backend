package com.codabli.dto;

import com.codabli.entity.enums.StatutModeration;
import com.codabli.entity.enums.TypeCarte;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une carte à conte.
 * <p>
 * Expose les informations essentielles sans exposer l'entité JPA directement.
 * Inclut les champs "createurNom" et "createurPrenom" pour afficher
 * le nom du créateur côté frontend sans requête supplémentaire.
 */
public record CarteAConteResponse(
        UUID id,
        TypeCarte type,
        String imageUrl,
        String texteAssocie,
        StatutModeration statutModeration,
        //Motif renseigné lors d'un refus ou d'une demande de correction (MOD-03/04)
        String motifModeration,
        OffsetDateTime dateCreation,
        UUID conteId,
        //ID du créateur (élève)
        UUID createurId,
        String createurNom,
        String createurPrenom) {
}
