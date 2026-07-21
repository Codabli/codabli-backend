package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de sortie pour une adresse de livraison.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseLivraisonResponse {

    private UUID id;
    private String nom;
    private String adresse;
    private String codePostal;
    private String ville;
    private String telephone;
    private boolean parDefaut;
}
