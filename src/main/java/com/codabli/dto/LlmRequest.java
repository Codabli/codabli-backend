package com.codabli.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Prompt structure envoye au LLM.
 * <p>
 * La temperature est facultative : si elle est {@code null}, le client applique
 * la valeur par defaut de la configuration ({@code codabli.llm.temperature}).
 * Le modele et le nombre maximal de tokens sont fixes par le backend.
 */
public record LlmRequest(
        @NotEmpty(message = "Au moins un message est requis")
        @Valid
        List<LlmMessage> messages,
        Double temperature) {

    public LlmRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        if (messages.stream().noneMatch(message -> LlmMessage.ROLE_USER.equalsIgnoreCase(message.role()))) {
            throw new IllegalArgumentException("Au moins un message utilisateur est requis");
        }
    }

    /** Raccourci pour une requete contenant un seul message utilisateur. */
    public static LlmRequest of(String user) {
        return new LlmRequest(List.of(LlmMessage.user(user)), null);
    }
}
