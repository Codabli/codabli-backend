package com.codabli.dto;

import java.util.List;
import java.util.Objects;

public record ConteContextData(
        String titre,
        String thematique,
        List<String> ingredientsSecrets,
        String pays,
        String culture,
        int ageMin,
        int ageMax) {

    public ConteContextData {
        ingredientsSecrets = ingredientsSecrets == null
                ? List.of()
                : ingredientsSecrets.stream()
                        .filter(Objects::nonNull)
                        .map(String::strip)
                        .filter(ingredient -> !ingredient.isEmpty())
                        .toList();
    }
}
