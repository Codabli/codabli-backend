package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requete du mode conversationnel persistant : le contexte est charge par le
 * backend et seul le nouveau message utilisateur est fourni par le client.
 */
public record AssistantIaRequest(
        @NotBlank(message = "Le message est obligatoire") String contenu,
        Double temperature) {
}
