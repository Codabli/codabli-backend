package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un profil enfant (ENF-01).
 * N'expose volontairement aucune information sur le compte du responsable
 * (RG-03) au-dela de son ID technique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilEnfantResponse {

    private UUID id;

    private UUID responsableId;

    private String pseudonyme;

    private LocalDate dateNaissance;

    private String languePreferee;

    private String preferences;

    private String accessibilite;

    private boolean autorisationParentale;

    private OffsetDateTime dateAutorisation;

    private boolean actif;

    private OffsetDateTime dateCreation;
}
