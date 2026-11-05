package com.codabli.service;

import com.codabli.config.LlmProperties;
import com.codabli.config.QwenGuardProperties;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OvhQwenGuardClient implements QwenGuardClient {

    private static final Logger log = LoggerFactory.getLogger(OvhQwenGuardClient.class);
    private static final Pattern SAFETY_PATTERN = Pattern.compile("(?im)^\\s*Safety:\\s*([^\\r\\n]+)");
    private static final String SYSTEM_PROMPT_PATH = "classpath:prompts/qwen-guard/system-prompt.txt";

    private final HttpClient httpClient;
    private final LlmProperties llmProperties;
    private final QwenGuardProperties qwenGuardProperties;
    private final String systemPrompt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OvhQwenGuardClient(HttpClient llmHttpClient,
                              LlmProperties llmProperties,
                              QwenGuardProperties qwenGuardProperties,
                              ResourceLoader resourceLoader) {
        this.httpClient = llmHttpClient;
        this.llmProperties = llmProperties;
        this.qwenGuardProperties = qwenGuardProperties;
        this.systemPrompt = chargerPromptSysteme(resourceLoader.getResource(SYSTEM_PROMPT_PATH));
    }

    @Override
    public SafetyAnalysis analyser(String prompt) {
        HttpRequest request = construireRequete(prompt);
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Appel QwenGuard interrompu");
            throw new QwenGuardIndisponibleException("Appel QwenGuard interrompu", e);
        } catch (IOException e) {
            log.warn("Appel QwenGuard impossible: {}", e.getClass().getSimpleName());
            throw new QwenGuardIndisponibleException("Appel QwenGuard impossible", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("QwenGuard a repondu avec le statut HTTP {}", response.statusCode());
            throw new QwenGuardIndisponibleException(
                    "QwenGuard a repondu avec le statut HTTP " + response.statusCode());
        }

        try {
            JsonNode choices = objectMapper.readTree(response.body()).path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("Reponse QwenGuard sans choix exploitable");
                throw new QwenGuardIndisponibleException("Reponse QwenGuard sans choix exploitable");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            return new SafetyAnalysis(analyserStatut(content), content);
        } catch (JsonProcessingException e) {
            log.warn("Reponse QwenGuard illisible: {}", e.getClass().getSimpleName());
            throw new QwenGuardIndisponibleException("Reponse QwenGuard illisible", e);
        }
    }

    private HttpRequest construireRequete(String prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", qwenGuardProperties.model());
        body.put("temperature", 0);
        body.put("max_tokens", qwenGuardProperties.maxTokens());
        body.putArray("messages")
                .addObject().put("role", "system").put("content", systemPrompt);
        body.withArray("messages").addObject().put("role", "user").put("content", prompt);

        try {
            String baseUrl = llmProperties.baseUrl().replaceAll("/+$", "");
            return HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(qwenGuardProperties.timeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + llmProperties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException e) {
            throw new QwenGuardIndisponibleException("Serialisation de la requete QwenGuard impossible", e);
        }
    }

    private static String chargerPromptSysteme(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            String prompt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (prompt.isBlank()) {
                throw new IllegalStateException("La ressource du prompt QwenGuard est vide");
            }
            return prompt;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger la ressource du prompt QwenGuard", e);
        }
    }

    private static SafetyStatus analyserStatut(String content) {
        Matcher matcher = SAFETY_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("Reponse QwenGuard sans statut de securite");
            throw new QwenGuardIndisponibleException("Reponse QwenGuard sans statut de securite");
        }

        return switch (matcher.group(1).strip().toUpperCase(java.util.Locale.ROOT)) {
            case "SAFE" -> SafetyStatus.SAFE;
            case "UNSAFE" -> SafetyStatus.UNSAFE;
            case "CONTROVERSIAL" -> SafetyStatus.CONTROVERSIAL;
            default -> {
                log.warn("Statut QwenGuard inconnu");
                throw new QwenGuardIndisponibleException("Statut QwenGuard inconnu");
            }
        };
    }
}
