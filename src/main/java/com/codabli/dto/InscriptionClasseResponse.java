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
public class InscriptionClasseResponse {

    private UUID id;
    private UUID eleveId;
    private String eleveNom;
    private String elevePrenom;
    private UUID classeId;
    private String classeNom;
    private OffsetDateTime dateInscription;
}
