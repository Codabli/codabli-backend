package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une traduction de Conte Danse
 * (LAN-01/02).
 */
public record ConteDanseTraductionRequest(
        @NotBlank(message = "La langue est obligatoire")
        @Size(max = 10)
        String langue,
        @Size(max = 50)
        String variante,
        String texte,
        String audioUrl,
        String videoUrl,
        String sousTitresUrl,
        UUID traducteurId,
        UUID relecteurId) {
}
