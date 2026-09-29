package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InscriptionClasseResponse(
        UUID id,
        UUID eleveId,
        String eleveNom,
        String elevePrenom,
        UUID classeId,
        String classeNom,
        OffsetDateTime dateInscription) {
}
