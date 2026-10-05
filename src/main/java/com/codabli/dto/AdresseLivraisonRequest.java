package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour la creation/modification d'une adresse de livraison.
 */
public record AdresseLivraisonRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 255)
        String nom,
        @NotBlank(message = "L'adresse est obligatoire")
        String adresse,
        @NotBlank(message = "Le code postal est obligatoire")
        @Size(max = 10)
        String codePostal,
        @NotBlank(message = "La ville est obligatoire")
        @Size(max = 100)
        String ville,
        @Size(max = 20)
        String telephone,
        Boolean parDefaut) {
}
