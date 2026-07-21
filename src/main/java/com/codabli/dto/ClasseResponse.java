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
public class ClasseResponse {

    private UUID id;
    private String nom;
    private String niveau;
    private String anneeScolaire;
    private UUID ecoleId;
    private String ecoleNom;
    private UUID enseignantId;
    private String enseignantNom;
    private OffsetDateTime dateCreation;
}
