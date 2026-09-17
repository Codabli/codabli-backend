package com.codabli.dto;

import java.util.UUID;

/**
 * DTO d'entree pour creer une commande a partir du panier.
 * L'adresse de livraison est optionnelle (null si produits numeriques
 * uniquement).
 */
public record CreerCommandeRequest(UUID adresseLivraisonId) {
}
