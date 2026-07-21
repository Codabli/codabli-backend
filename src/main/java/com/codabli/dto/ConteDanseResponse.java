package com.codabli.dto;

import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un conte danse.
 * Inclut les champs "createurNom" et "createurPrenom" pour le frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanseResponse {

    private UUID id;

    private String titre;

    private String description;

    private String thematique;

    private String langueOriginale;

    private StatutConte statut;

    private AccesConte acces;

    private String isbn;

    private String fichierTexteUrl;

    private String fichierAudioUrl;

    private String fichierVideoUrl;

    private OffsetDateTime dateCreation;

    private OffsetDateTime datePublication;

    /** ID du createur */
    private UUID createurId;

    /** Nom du createur */
    private String createurNom;

    /** Prenom du createur */
    private String createurPrenom;

    /** ID de l'ecole associee */
    private UUID ecoleId;

    /** ID de la classe associee */
    private UUID classeId;
}
