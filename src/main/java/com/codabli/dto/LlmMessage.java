package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Message structure d'un echange avec le LLM (format role / contenu, commun a
 * l'API compatible OpenAI d'OVHcloud AI Endpoints).
 * <p>
 * Les roles attendus sont {@code system}, {@code user} et {@code assistant}.
 * Les fabriques statiques evitent les fautes de frappe sur le role.
 */
public record LlmMessage(
        @NotNull @NotBlank String role,
        @NotNull @NotBlank String content) {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public static LlmMessage system(String content) {
        return new LlmMessage(ROLE_SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(ROLE_USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(ROLE_ASSISTANT, content);
    }
}
