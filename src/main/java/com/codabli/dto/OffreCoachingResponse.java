package com.codabli.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OffreCoachingResponse(
        UUID id,
        String titre,
        String description,
        Integer dureeMinutes,
        BigDecimal tarif,
        boolean actif) {
}
