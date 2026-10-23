package com.codabli.service;

import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;

import java.util.function.Consumer;

/**
 * Client de communication avec le service LLM souverain (EA-107).
 * <p>
 * Interface agnostique au fournisseur et au modele : l'appelant ne connait ni
 * l'URL, ni le SDK, ni le format de fil. L'implementation cible OVHcloud AI
 * Endpoints (API compatible OpenAI).
 */
public interface LlmClient {

    /**
     * Appel synchrone : renvoie la reponse complete d'un seul coup.
     * <p>
     * Ne leve pas d'exception sur indisponibilite du LLM : renvoie une reponse de
     * repli ({@link LlmResponse#standby(String)}) marquee {@code degrade} (critere 6).
     */
    LlmResponse complete(LlmRequest request);

    /**
     * Appel en streaming : invoque {@code onToken} pour chaque fragment de contenu
     * recu, au fur et a mesure de la generation.
     * <p>
     * En cas d'indisponibilite, {@code onToken} recoit le message de repli puis la
     * methode se termine normalement (critere 6).
     *
     * @param onToken consommateur appele pour chaque fragment de texte
     */
    LlmResponse stream(LlmRequest request, Consumer<String> onToken);
}
