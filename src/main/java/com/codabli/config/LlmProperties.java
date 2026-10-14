package com.codabli.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration du client LLM (EA-107).
 * <p>
 * Toutes les valeurs sont externalisees ({@code codabli.llm.*} dans
 * {@code application.yml}). La cle API provient d'une variable d'environnement et
 * n'est jamais ecrite en dur ni journalisee (critere 3).
 * <p>
 * Souverainete : {@code baseUrl} doit pointer exclusivement vers un endpoint
 * OVHcloud AI Endpoints heberge dans l'UE (critere 5). La validation du prefixe
 * est assuree au demarrage par {@link LlmClientConfig}.
 *
 * @param baseUrl     base URL de l'API compatible OpenAI (OVH AI Endpoints)
 * @param apiKey      cle API (injectee via variable d'environnement)
 * @param model       modele par defaut (surcharged par requete si besoin)
 * @param temperature temperature par defaut
 * @param maxTokens   plafond de tokens de sortie par defaut
 * @param timeout     timeout explicite par appel (pas de retry silencieux)
 * @param standbyMessage message de repli affiche a l'enfant si le LLM est indisponible
 */
@Validated
@ConfigurationProperties(prefix = "codabli.llm")
public record LlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotNull Double temperature,
        @NotNull @Positive Integer maxTokens,
        @NotNull Duration timeout,
        @NotBlank String standbyMessage) {
}
