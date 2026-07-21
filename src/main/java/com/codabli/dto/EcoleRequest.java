package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcoleRequest {

    @NotBlank(message = "Le nom de l'ecole est obligatoire")
    @Size(max = 200)
    private String nom;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(max = 100)
    private String pays;

    @Size(max = 100)
    private String ville;
}
