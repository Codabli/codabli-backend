package com.codabli.service;

import com.codabli.config.LlmProperties;
import com.codabli.config.QwenGuardProperties;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OvhQwenGuardClientTest {

    private static final String PROMPT = "prompt anonymise";
    private final AtomicReference<Integer> responseStatus = new AtomicReference<>(200);
    private final AtomicReference<String> responseContent = new AtomicReference<>("""
            {"choices":[{"message":{"content":"Safety: Safe\\nCategories: none"}}]}""");
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private HttpServer server;
    private OvhQwenGuardClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = responseContent.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1/";
        LlmProperties llmProperties = new LlmProperties(baseUrl, "test-api-key", "llm-model",
                new ByteArrayResource("system prompt".getBytes(StandardCharsets.UTF_8)),
                0.3, 256, Duration.ofSeconds(5), "standby");
        client = new OvhQwenGuardClient(HttpClient.newHttpClient(), llmProperties,
                new QwenGuardProperties("Qwen3Guard-Gen-0.6B", Duration.ofSeconds(5)));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void analyser_shouldSendPromptToConfiguredOvhEndpointAndParseSafeStatus() throws Exception {
        QwenGuardClient.SafetyAnalysis result = client.analyser(PROMPT);
        assertThat(result.status()).isEqualTo(SafetyStatus.SAFE);
        assertThat(result.details()).contains("Safety: Safe");

        JsonNode request = new ObjectMapper().readTree(requestBody.get());
        assertThat(request.path("model").asText()).isEqualTo("Qwen3Guard-Gen-0.6B");
        assertThat(request.path("temperature").asInt()).isZero();
        assertThat(request.path("messages").get(0).path("content").asText()).isEqualTo(PROMPT);
        assertThat(authorization.get()).isEqualTo("Bearer test-api-key");
    }

    @Test
    void analyser_shouldParseUnsafeStatus() {
        responseContent.set("""
                {"choices":[{"message":{"content":"Safety: Unsafe\\nCategories: violence"}}]}""");

        QwenGuardClient.SafetyAnalysis result = client.analyser(PROMPT);
        assertThat(result.status()).isEqualTo(SafetyStatus.UNSAFE);
        assertThat(result.details()).contains("Categories: violence");
    }

    @Test
    void analyser_shouldParseControversedStatus() {
        responseContent.set("""
                {"choices":[{"message":{"content":"Safety: Controversed\\nCategories: sensitive"}}]}""");

        QwenGuardClient.SafetyAnalysis result = client.analyser(PROMPT);
        assertThat(result.status()).isEqualTo(SafetyStatus.CONTROVERSIAL);
        assertThat(result.details()).contains("Categories: sensitive");
    }

    @Test
    void analyser_shouldRejectHttpErrors() {
        responseStatus.set(503);
        responseContent.set("provider error");

        assertThatThrownBy(() -> client.analyser(PROMPT))
                .isInstanceOf(QwenGuardIndisponibleException.class)
                .hasMessageContaining("statut HTTP 503");
    }

    @Test
    void analyser_shouldRejectMalformedOrUnknownSafetyResponses() {
        responseContent.set("not-json");
        assertThatThrownBy(() -> client.analyser(PROMPT))
                .isInstanceOf(QwenGuardIndisponibleException.class)
                .hasMessage("Reponse QwenGuard illisible");

        responseContent.set("""
                {"choices":[{"message":{"content":"No safety status"}}]}""");
        assertThatThrownBy(() -> client.analyser(PROMPT))
                .isInstanceOf(QwenGuardIndisponibleException.class)
                .hasMessage("Reponse QwenGuard sans statut de securite");

        responseContent.set("""
                {"choices":[{"message":{"content":"Safety: NewStatus"}}]}""");
        assertThatThrownBy(() -> client.analyser(PROMPT))
                .isInstanceOf(QwenGuardIndisponibleException.class)
                .hasMessage("Statut QwenGuard inconnu");
    }
}
