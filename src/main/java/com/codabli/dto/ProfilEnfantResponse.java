package com.codabli.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un profil enfant (ENF-01).
 * N'expose volontairement aucune information sur le compte du responsable
 * (RG-03) au-dela de son ID technique.
 */
public record ProfilEnfantResponse(
        UUID id,
        UUID responsableId,
        String pseudonyme,
        LocalDate dateNaissance,
        String languePreferee,
        String preferences,
        String accessibilite,
        boolean autorisationParentale,
        OffsetDateTime dateAutorisation,
        boolean actif,
        OffsetDateTime dateCreation) {
}
