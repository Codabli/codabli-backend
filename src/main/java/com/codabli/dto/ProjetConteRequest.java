package com.codabli.dto;

import com.codabli.entity.enums.EspaceRepresentation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Contexte du projet saisi a l'ecran 1 du parcours "Creer mon conte".
 * <p>
 * RG-CMC-01 : tranche d'age, pays, region et theme obligatoires.
 * RG-CMC-02 : ingredients secrets = mots-cles libres.
 */
public record ProjetConteRequest(
        @NotBlank(message = "La tranche d'age est obligatoire")
        @Pattern(regexp = "3-5|6-8|9-11|12-14|15-18",
                message = "La tranche d'age doit etre 3-5, 6-8, 9-11, 12-14 ou 15-18")
        String trancheAge,
        @NotBlank(message = "Le pays est obligatoire")
        @Size(max = 100)
        String pays,
        @NotBlank(message = "La region est obligatoire")
        @Size(max = 100)
        String region,
        @Size(max = 100)
        String ville,
        @NotBlank(message = "Le theme est obligatoire")
        @Size(max = 150)
        String theme,
        @Size(max = 20, message = "20 ingredients secrets maximum")
        List<@NotBlank @Size(max = 40, message = "Un ingredient secret fait 40 caracteres maximum") String> ingredientsSecrets,
        EspaceRepresentation espaceRepresentation) {
}
