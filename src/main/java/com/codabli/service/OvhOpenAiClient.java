package com.codabli.service;

import com.codabli.config.LlmProperties;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Implementation du {@link LlmClient} ciblant OVHcloud AI Endpoints (API
 * compatible OpenAI). Voir EA-107.
 * <p>
 * Principes (herites du prototype Python {@code providers.py}) :
 * <ul>
 *   <li>timeout explicite, aucun retry silencieux ;</li>
 *   <li>jamais de contenu de prompt/reponse dans les logs — uniquement des
 *       metadonnees (modele, tokens, latence, finish_reason, id d'appel) ;</li>
 *   <li>degradation gracieuse : une panne du LLM renvoie une reponse de repli,
 *       jamais une exception propagee au client.</li>
 * </ul>
 */
@Service
public class OvhOpenAiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OvhOpenAiClient.class);
    private static final String DONE = "[DONE]";

    // Mapper dedie au format de fil OVH (compatible OpenAI). Instancie en interne
    // plutot qu'injecte : Spring Boot 4.1 expose Jackson 3 (tools.jackson), pas
    // l'ObjectMapper Jackson 2 utilise ici — et cet usage reste isole du reste.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final LlmProperties properties;

    public OvhOpenAiClient(HttpClient llmHttpClient, LlmProperties properties) {
        this.httpClient = llmHttpClient;
        this.properties = properties;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String appelId = nouvelAppelId();
        String model = modele(request);
        long t0 = System.nanoTime();
        try {
            HttpRequest httpRequest = construireRequete(request, model, false);
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            verifierStatut(response.statusCode());

            JsonNode racine = objectMapper.readTree(response.body());
            JsonNode choix = premierChoix(racine);
            JsonNode message = choix.path("message");
            String texte = message.path("content").asText("");
            String reasoning = message.path("reasoning").asText("");
            String finishReason = texteOuNull(choix.path("finish_reason"));
            Integer tokensIn = entierOuNull(racine.path("usage").path("prompt_tokens"));
            Integer tokensOut = entierOuNull(racine.path("usage").path("completion_tokens"));

            loguerSucces("complete", appelId, model, tokensIn, tokensOut, finishReason, t0);
            return LlmResponse.ok(texte.strip(), reasoning.strip(), finishReason,
                    tokensIn, tokensOut);
        } catch (Exception e) {
            return replierSurStandby("complete", appelId, model, t0, e);
        }
    }

    @Override
    public void stream(LlmRequest request, Consumer<String> onToken) {
        String appelId = nouvelAppelId();
        String model = modele(request);
        long t0 = System.nanoTime();
        String finishReason = null;
        Integer tokensIn = null;
        Integer tokensOut = null;
        try {
            HttpRequest httpRequest = construireRequete(request, model, true);
            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            verifierStatut(response.statusCode());

            try (BufferedReader lecteur = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String ligne;
                while ((ligne = lecteur.readLine()) != null) {
                    if (!ligne.startsWith("data:")) {
                        continue; // lignes vides / commentaires SSE
                    }
                    String charge = ligne.substring("data:".length()).strip();
                    if (charge.isEmpty()) {
                        continue;
                    }
                    if (DONE.equals(charge)) {
                        break;
                    }
                    JsonNode chunk = objectMapper.readTree(charge);
                    JsonNode usage = chunk.path("usage");
                    if (usage.isObject()) {
                        tokensIn = entierOuNull(usage.path("prompt_tokens"));
                        tokensOut = entierOuNull(usage.path("completion_tokens"));
                    }
                    JsonNode choix = chunk.path("choices");
                    if (!choix.isArray() || choix.isEmpty()) {
                        continue;
                    }
                    JsonNode premier = choix.get(0);
                    String fr = texteOuNull(premier.path("finish_reason"));
                    if (fr != null) {
                        finishReason = fr;
                    }
                    String fragment = premier.path("delta").path("content").asText("");
                    if (!fragment.isEmpty()) {
                        onToken.accept(fragment);
                    }
                }
            }
            loguerSucces("stream", appelId, model, tokensIn, tokensOut, finishReason, t0);
        } catch (Exception e) {
            loguerEchec("stream", appelId, model, t0, e);
            // Degradation gracieuse : on pousse le message de repli dans le flux.
            onToken.accept(properties.standbyMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Construction de la requete (format compatible OpenAI)
    // ────────────────────────────────────────────────────────────────

    private HttpRequest construireRequete(LlmRequest request, String model, boolean stream) {
        ObjectNode corps = objectMapper.createObjectNode();
        corps.put("model", model);
        corps.put("temperature",
                request.temperature() != null ? request.temperature() : properties.temperature());
        corps.put("max_tokens",
                request.maxTokens() != null ? request.maxTokens() : properties.maxTokens());
        corps.put("stream", stream);
        if (stream) {
            corps.set("stream_options",
                    objectMapper.createObjectNode().put("include_usage", true));
        }
        ArrayNode messages = corps.putArray("messages");
        for (LlmMessage m : request.messages()) {
            messages.addObject().put("role", m.role()).put("content", m.content());
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(corps);
        } catch (Exception e) {
            throw new LlmIndisponibleException("Serialisation de la requete LLM impossible", e);
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/chat/completions"))
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", stream ? "text/event-stream" : "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // Utilitaires
    // ────────────────────────────────────────────────────────────────

    private String modele(LlmRequest request) {
        return (request.model() != null && !request.model().isBlank())
                ? request.model() : properties.model();
    }

    private void verifierStatut(int statut) {
        if (statut < 200 || statut >= 300) {
            // On ne logue/propage que le code HTTP, jamais le corps : il peut
            // contenir le prompt renvoye en echo (RGPD, critere 4).
            throw new LlmIndisponibleException("LLM a repondu avec le statut HTTP " + statut);
        }
    }

    private JsonNode premierChoix(JsonNode racine) {
        JsonNode choix = racine.path("choices");
        if (!choix.isArray() || choix.isEmpty()) {
            throw new LlmIndisponibleException("Reponse LLM sans champ 'choices' exploitable");
        }
        return choix.get(0);
    }

    private static String texteOuNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static Integer entierOuNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private static String nouvelAppelId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private LlmResponse replierSurStandby(String mode, String appelId, String model,
                                          long t0, Exception e) {
        loguerEchec(mode, appelId, model, t0, e);
        return LlmResponse.standby(properties.standbyMessage());
    }

    // ────────────────────────────────────────────────────────────────
    // Journalisation — METADONNEES UNIQUEMENT (jamais de contenu, RGPD)
    // ────────────────────────────────────────────────────────────────

    private void loguerSucces(String mode, String appelId, String model, Integer tokensIn,
                              Integer tokensOut, String finishReason, long t0) {
        log.info("LLM {} ok [appel={}] model={} tokensIn={} tokensOut={} finish={} dureeMs={}",
                mode, appelId, model, tokensIn, tokensOut, finishReason, dureeMs(t0));
    }

    private void loguerEchec(String mode, String appelId, String model, long t0, Exception e) {
        // On logue le type d'erreur et un message technique maitrise, pas l'exception
        // complete : une stacktrace ou un message de SDK peut echoer le prompt.
        log.warn("LLM {} indisponible [appel={}] model={} dureeMs={} cause={}: {}",
                mode, appelId, model, dureeMs(t0), e.getClass().getSimpleName(),
                messageTechnique(e));
    }

    private static String messageTechnique(Exception e) {
        // Seules nos propres exceptions ont un message garanti sans contenu.
        return (e instanceof LlmIndisponibleException) ? e.getMessage() : "erreur reseau/IO";
    }

    private static long dureeMs(long t0) {
        return Duration.ofNanos(System.nanoTime() - t0).toMillis();
    }
}
