package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClasseResponse(
        UUID id,
        String nom,
        String niveau,
        String anneeScolaire,
        UUID ecoleId,
        String ecoleNom,
        UUID enseignantId,
        String enseignantNom,
        OffsetDateTime dateCreation) {
}
