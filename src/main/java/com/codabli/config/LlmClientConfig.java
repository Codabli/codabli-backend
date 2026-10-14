package com.codabli.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration du client LLM : client HTTP, executor et garde-fou de souverainete.
 * <p>
 * Le {@link HttpClient} JDK est utilise plutot qu'un SDK fournisseur : timeout
 * explicite, aucun retry automatique sur les POST (un blocage reste detectable —
 * regle heritee du prototype Python), et aucune dependance supplementaire.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmClientConfig {

    public LlmClientConfig(LlmProperties properties) {
        // Garde-fou souverainete (EA-107, critere 5) : refuser de demarrer si la
        // base URL n'est pas un endpoint OVHcloud en HTTPS. Empeche tout appel
        // accidentel hors du perimetre souverain (ex. api.mistral.ai).
        String baseUrl = properties.baseUrl();
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "codabli.llm.base-url doit etre en HTTPS (perimetre souverain) : " + baseUrl);
        }
        if (!baseUrl.contains(".ai.cloud.ovh.net")) {
            throw new IllegalStateException(
                    "codabli.llm.base-url doit cibler OVHcloud AI Endpoints (*.ai.cloud.ovh.net) : "
                            + baseUrl);
        }
    }

    /**
     * Executor a threads virtuels (Java 25) pour piloter le streaming SSE sans
     * monopoliser les threads de requete du serveur.
     */
    @Bean(destroyMethod = "close")
    public ExecutorService llmExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public HttpClient llmHttpClient(LlmProperties properties, ExecutorService llmExecutor) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .executor(llmExecutor)
                .build();
    }
}
