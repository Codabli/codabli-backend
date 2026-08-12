package com.codabli.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCoachingRequest {

    @NotNull(message = "L'offre de coaching est obligatoire")
    private UUID offreCoachingId;

    @NotNull(message = "Le creneau est obligatoire")
    private OffsetDateTime dateCreneau;
}
