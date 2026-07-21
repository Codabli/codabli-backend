package com.codabli.service;

/**
 * Exception levee lorsque le stock d'un produit est insuffisant pour la
 * commande.
 */
public class StockInsuffisantException extends RuntimeException {

    public StockInsuffisantException(String message) {
        super(message);
    }
}
