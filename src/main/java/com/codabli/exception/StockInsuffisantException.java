package com.codabli.exception;

/**
 * Exception levee lorsque le stock d'un produit est insuffisant pour la
 * commande.
 */
public class StockInsuffisantException extends BusinessException {

    public StockInsuffisantException(String nom, Integer valeur, Integer attendu) {
        super(
                ErrorCode.SOCK_INSUFFISANT,
                "Stock insuffisant pour le produit '" + nom +  "'. Stock disponible: " + valeur + ", quantite demandee: " + attendu
        );
    }
}
