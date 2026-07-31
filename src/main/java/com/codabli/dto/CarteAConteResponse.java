package com.codabli.dto;

import com.codabli.entity.enums.StatutModeration;
import com.codabli.entity.enums.TypeCarte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une carte à conte.
 *
 * Expose les informations essentielles sans exposer l'entité JPA directement.
 * Inclut les champs "createurNom" et "createurPrenom" pour afficher
 * le nom du créateur côté frontend sans requête supplémentaire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteAConteResponse {

    private UUID id;

    private TypeCarte type;

    private String imageUrl;

    private String texteAssocie;

    private StatutModeration statutModeration;

    /** Motif renseigné lors d'un refus ou d'une demande de correction (MOD-03/04) */
    private String motifModeration;

    private OffsetDateTime dateCreation;

    private UUID conteId;

    /** ID du créateur (élève) */
    private UUID createurId;

    /** Nom du créateur — évite un appel API supplémentaire côté client */
    private String createurNom;

    /** Prénom du créateur */
    private String createurPrenom;
}
