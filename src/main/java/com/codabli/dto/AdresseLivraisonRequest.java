package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour la creation/modification d'une adresse de livraison.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseLivraisonRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255)
    private String nom;

    @NotBlank(message = "L'adresse est obligatoire")
    private String adresse;

    @NotBlank(message = "Le code postal est obligatoire")
    @Size(max = 10)
    private String codePostal;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100)
    private String ville;

    @Size(max = 20)
    private String telephone;

    private Boolean parDefaut;
}
