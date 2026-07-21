package com.codabli.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionClasseRequest {

    @NotNull(message = "L'ID de l'eleve est obligatoire")
    private UUID eleveId;

    @NotNull(message = "L'ID de la classe est obligatoire")
    private UUID classeId;
}
