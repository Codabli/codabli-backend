package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreCoachingResponse {

    private UUID id;

    private String titre;

    private String description;

    private Integer dureeMinutes;

    private BigDecimal tarif;

    private boolean actif;
}
