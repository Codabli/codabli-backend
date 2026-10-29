package com.codabli.dto;

import com.codabli.entity.enums.EspaceRepresentation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Contexte du projet propre au parcours (ecran 1). Pays, thematique et
 * tranche d'age sont portes par le conte danse lui-meme (ConteDanseRequest).
 */
public record ParcoursContexteRequest(
        @NotBlank(message = "La region est obligatoire")
        @Size(max = 150)
        String region,
        @Size(max = 150)
        String ville,
        EspaceRepresentation espaceRepresentation,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 40) String> ingredientsSecrets) {
}
