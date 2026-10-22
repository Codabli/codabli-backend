package com.codabli.service;

/**
 * Levee en interne lorsque l'appel au LLM echoue (indisponibilite, timeout,
 * statut HTTP non 2xx, reponse illisible).
 * <p>
 * Le message ne contient jamais le contenu du prompt ni de la reponse (RGPD,
 * EA-107 critere 4) — uniquement une cause technique. {@link OvhOpenAiClient}
 * la capture pour renvoyer une reponse de repli (standby).
 */
public class LlmIndisponibleException extends RuntimeException {

    public LlmIndisponibleException(String message) {
        super(message);
    }

    public LlmIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
