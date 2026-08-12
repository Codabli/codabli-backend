package com.codabli.dto;

import com.codabli.entity.enums.StatutReservationCoaching;
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
public class ReservationCoachingResponse {

    private UUID id;

    private UUID offreCoachingId;

    private String offreCoachingTitre;

    private OffsetDateTime dateCreneau;

    private StatutReservationCoaching statut;

    private OffsetDateTime dateCreation;
}
