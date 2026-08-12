package com.codabli.dto;

import com.codabli.entity.enums.TypeCibleMiseEnAvant;
import jakarta.validation.constraints.NotNull;
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
public class MiseEnAvantRequest {

    @NotNull(message = "Le type de cible est obligatoire")
    private TypeCibleMiseEnAvant typeCible;

    @NotNull(message = "L'identifiant de la cible est obligatoire")
    private UUID cibleId;

    private String titre;

    private OffsetDateTime dateDebut;

    private OffsetDateTime dateFin;

    private Integer ordreAffichage;

    private Boolean actif;
}
