package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO d'entree pour la creation/modification d'une traduction de Conte Danse
 * (LAN-01/02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanseTraductionRequest {

    @NotBlank(message = "La langue est obligatoire")
    @Size(max = 10)
    private String langue;

    @Size(max = 50)
    private String variante;

    private String texte;

    private String audioUrl;

    private String videoUrl;

    private String sousTitresUrl;

    private UUID traducteurId;

    private UUID relecteurId;
}
