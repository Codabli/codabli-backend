package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Message structure d'un echange avec le LLM (format role / contenu, commun a
 * l'API compatible OpenAI d'OVHcloud AI Endpoints).
 * Les trois roles principaux sont :
 * - system : definit les consignes, le rôle et le contexte (contient le system_prompt par défaut);
 * - user : porte la demande ou le message transmis au LLM ;
 * - assistant : porte une reponse precedente du LLM dans l'echange.
 *
 * Les valeurs envoyees a l'API sont en minuscules. 
 *  */
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
