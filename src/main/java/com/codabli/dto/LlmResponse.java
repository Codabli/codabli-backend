package com.codabli.dto;

/**
 * Reponse unifiee du LLM, independante du fournisseur.
 * <p>
 * Transposition du dictionnaire renvoye par le prototype Python
 * {@code codabli-ai/banc-essai/providers.py}.
 *
 * @param texte     contenu visible par l'utilisateur
 * @param reasoning tokens de reflexion (modeles reasoning) — chaine vide sinon
 * @param finishReason raison d'arret renvoyee par le modele ({@code stop},
 *                     {@code length}, ...), ou {@code null}
 * @param tronque   vrai si la reponse a ete coupee par la limite de tokens
 * @param tokensIn  tokens consommes en entree, ou {@code null} si inconnu
 * @param tokensOut tokens generes en sortie, ou {@code null} si inconnu
 * @param degrade   vrai si c'est une reponse de repli (standby) suite a une
 *                  indisponibilite du LLM, et non une vraie reponse du modele
 */
public record LlmResponse(
        String texte,
        String reasoning,
        String finishReason,
        boolean tronque,
        Integer tokensIn,
        Integer tokensOut,
        boolean degrade) {

    /** Reponse nominale du modele. */
    public static LlmResponse ok(String texte, String reasoning, String finishReason,
                                 Integer tokensIn, Integer tokensOut) {
        boolean tronque = "length".equals(finishReason);
        return new LlmResponse(texte, reasoning == null ? "" : reasoning, finishReason,
                tronque, tokensIn, tokensOut, false);
    }

    /** Reponse de repli (standby) lorsque le LLM est indisponible (EA-107, critere 6). */
    public static LlmResponse standby(String message) {
        return new LlmResponse(message, "", null, false, null, null, true);
    }
}
