package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une entree du journal d'activite (SUP-03).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalActiviteResponse {

    private UUID id;

    private UUID acteurId;

    private String acteurNom;

    private String action;

    private String cible;

    private String details;

    private OffsetDateTime dateAction;
}
