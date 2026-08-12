package com.codabli.dto;

import com.codabli.entity.enums.TypeCibleMiseEnAvant;
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
public class MiseEnAvantResponse {

    private UUID id;

    private TypeCibleMiseEnAvant typeCible;

    private UUID cibleId;

    private String titre;

    private OffsetDateTime dateDebut;

    private OffsetDateTime dateFin;

    private int ordreAffichage;

    private boolean actif;
}
