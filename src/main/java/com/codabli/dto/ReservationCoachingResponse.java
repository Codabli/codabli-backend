package com.codabli.dto;

import com.codabli.entity.enums.StatutReservationCoaching;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationCoachingResponse(
        UUID id,
        UUID offreCoachingId,
        String offreCoachingTitre,
        OffsetDateTime dateCreneau,
        StatutReservationCoaching statut,
        OffsetDateTime dateCreation) {
}
