package com.codabli.dto;

import com.codabli.entity.enums.StatutTraduction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une traduction de Conte Danse (LAN-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanseTraductionResponse {

    private UUID id;

    private UUID conteId;

    private String langue;

    private String variante;

    private String texte;

    private String audioUrl;

    private String videoUrl;

    private String sousTitresUrl;

    private UUID traducteurId;

    private String traducteurNom;

    private UUID relecteurId;

    private String relecteurNom;

    private StatutTraduction statut;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
