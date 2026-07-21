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
public class EcoleResponse {

    private UUID id;
    private String nom;
    private String pays;
    private String ville;
    private String niveauAbonnement;
    private String statut;
    private OffsetDateTime dateCreation;
}
