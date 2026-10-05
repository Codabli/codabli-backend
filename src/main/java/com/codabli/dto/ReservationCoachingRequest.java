package com.codabli.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationCoachingRequest(
        @NotNull(message = "L'offre de coaching est obligatoire")
        UUID offreCoachingId,
        @NotNull(message = "Le creneau est obligatoire")
        OffsetDateTime dateCreneau) {
}
