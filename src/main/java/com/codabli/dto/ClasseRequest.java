package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseRequest {

    @NotNull(message = "L'ID de l'ecole est obligatoire")
    private UUID ecoleId;

    @NotNull(message = "L'ID de l'enseignant est obligatoire")
    private UUID enseignantId;

    @NotBlank(message = "Le nom de la classe est obligatoire")
    @Size(max = 150)
    private String nom;

    @Size(max = 50)
    private String niveau;

    @NotBlank(message = "L'annee scolaire est obligatoire")
    @Size(max = 20)
    private String anneeScolaire;
}
