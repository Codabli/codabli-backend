package com.codabli.dto;

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
public class InscriptionWebinaireResponse {

    private UUID id;

    private UUID webinaireId;

    private String webinaireTitre;

    private OffsetDateTime webinaireDateDebut;

    private boolean presenceConfirmee;

    private String attestationUrl;

    private OffsetDateTime dateInscription;
}
