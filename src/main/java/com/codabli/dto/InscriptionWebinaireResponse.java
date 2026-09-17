package com.codabli.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InscriptionWebinaireResponse(
        UUID id,
        UUID webinaireId,
        String webinaireTitre,
        OffsetDateTime webinaireDateDebut,
        boolean presenceConfirmee,
        String attestationUrl,
        OffsetDateTime dateInscription) {
}
