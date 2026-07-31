package com.codabli.dto;

import com.codabli.entity.enums.StatutAbonnement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une souscription (ABO-03/04).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbonnementResponse {

    private UUID id;

    private UUID offreId;

    private String offreNom;

    private String offreCode;

    private StatutAbonnement statut;

    private OffsetDateTime dateDebut;

    private OffsetDateTime dateFin;

    private boolean renouvellementAutomatique;

    private String factureUrl;

    private OffsetDateTime dateCreation;
}
