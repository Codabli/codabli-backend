package com.codabli.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Prompt structure envoye au LLM.
 * <p>
 * {@code model}, {@code temperature} et {@code maxTokens} sont facultatifs : s'ils
 * sont {@code null}, le client applique les valeurs par defaut de la configuration
 * ({@code codabli.llm.*}). Le client reste ainsi agnostique au modele — changer de
 * modele = changer un parametre, sans toucher au code appelant (EA-107, critere 1).
 */
public record LlmRequest(
        @NotEmpty(message = "Au moins un message est requis")
        @Valid
        List<LlmMessage> messages,
        String model,
        Double temperature,
        Integer maxTokens) {

    public LlmRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    /** Raccourci : un prompt systeme + un message utilisateur. */
    public static LlmRequest of(String system, String user) {
        return new LlmRequest(List.of(LlmMessage.system(system), LlmMessage.user(user)),
                null, null, null);
    }
}
