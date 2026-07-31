package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'un conte danse.
 *
 * Le createur n'est PAS dans le DTO — il est extrait du JWT
 * dans le service (principe de securite).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanseRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String titre;

    private String description;

    @Size(max = 150)
    private String thematique;

    @Size(max = 10)
    private String langueOriginale;

    private String couvertureUrl;

    @Size(max = 100)
    private String pays;

    @Size(max = 150)
    private String culture;

    private Integer ageMin;

    private Integer ageMax;

    private Integer dureeMinutes;

    private String credits;

    private String fichierTexteUrl;

    private String fichierAudioUrl;

    private String fichierVideoUrl;

    private UUID ecoleId;

    private UUID classeId;
}
