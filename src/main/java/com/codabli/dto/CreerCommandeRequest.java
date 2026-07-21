package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO d'entree pour creer une commande a partir du panier.
 * L'adresse de livraison est optionnelle (null si produits numeriques
 * uniquement).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreerCommandeRequest {

    private UUID adresseLivraisonId;
}
