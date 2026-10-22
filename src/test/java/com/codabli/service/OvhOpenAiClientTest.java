package com.codabli.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codabli.config.LlmProperties;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du client LLM contre un stub HTTP local.
 * <p>
 * Le client est instancie directement (sans passer par la config Spring), ce qui
 * contourne volontairement le garde-fou de souverainete — celui-ci est teste
 * separement et n'a pas de sens face a un stub localhost.
 */
class OvhOpenAiClientTest {

    private static final String PROMPT_SECRET = "SECRET-PROMPT-NE-DOIT-PAS-FUIR-42";
    private static final String SYSTEM_PROMPT = "Tu es un assistant pedagogique.";
    private static final String STANDBY = "Message de repli enfant";

    private HttpServer server;
    private OvhOpenAiClient client;
    private ListAppender<ILoggingEvent> logs;
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1";
        LlmProperties props = new LlmProperties(baseUrl, "cle-test", "modele-test", SYSTEM_PROMPT, 0.3, 256,
                Duration.ofSeconds(5), STANDBY);

        client = new OvhOpenAiClient(HttpClient.newHttpClient(), props);

        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(OvhOpenAiClient.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        ((Logger) LoggerFactory.getLogger(OvhOpenAiClient.class)).detachAppender(logs);
    }

    @Test
    void complete_renvoie_le_contenu_du_modele() {
        repondre200("""
                {"choices":[{"message":{"content":"Il etait une fois..."},
                 "finish_reason":"stop"}],
                 "usage":{"prompt_tokens":12,"completion_tokens":5}}""");

        LlmResponse response = client.complete(LlmRequest.of(PROMPT_SECRET));

        assertThat(response.degrade()).isFalse();
        assertThat(response.texte()).isEqualTo("Il etait une fois...");
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.tokensIn()).isEqualTo(12);
        assertThat(response.tokensOut()).isEqualTo(5);
        assertSystemPromptFirst();
    }

    @Test
    void complete_renvoie_le_standby_quand_le_llm_echoue() {
        repondre(500, "erreur interne cote fournisseur");

        LlmResponse response = client.complete(LlmRequest.of(PROMPT_SECRET));

        assertThat(response.degrade()).isTrue();
        assertThat(response.texte()).isEqualTo(STANDBY);
    }

    @Test
    void stream_pousse_les_fragments_au_fur_et_a_mesure() {
        repondreSse(List.of("data: {\"choices\":[{\"delta\":{\"content\":\"Bonjour \"}}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"petit\"}}]}", "data: [DONE]"));

        List<String> fragments = new ArrayList<>();
        client.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly("Bonjour ", "petit");
        assertSystemPromptFirst();
    }

    @Test
    void stream_pousse_le_standby_quand_le_llm_echoue() {
        repondre(503, "service indisponible");

        List<String> fragments = new ArrayList<>();
        client.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly(STANDBY);
    }

    @Test
    void stream_interrompt_la_lecture_du_corps_apres_le_timeout() {
        remplacerHandler(exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(("data: {\"choices\":[{\"delta\":{\"content\":\"debut\"}}]}\n\n").getBytes(
                        StandardCharsets.UTF_8));
                os.flush();
                Thread.sleep(3000);
            }
        });
        LlmProperties shortTimeout = new LlmProperties("http://localhost:" + server.getAddress().getPort() + "/v1",
                "cle-test", "modele-test", "system-prompt", 0.3, 256, Duration.ofSeconds(1), STANDBY);
        OvhOpenAiClient timeoutClient = new OvhOpenAiClient(HttpClient.newHttpClient(), shortTimeout);

        List<String> fragments = new ArrayList<>();
        long startedAt = System.nanoTime();
        timeoutClient.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(fragments).containsExactly("debut", STANDBY);
        assertThat(elapsedMillis).isLessThan(2500);
    }

    @Test
    void aucun_contenu_de_prompt_ni_de_reponse_dans_les_logs() {
        repondre200("""
                {"choices":[{"message":{"content":"REPONSE-MODELE-CONFIDENTIELLE"},
                 "finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1}}""");

        client.complete(new LlmRequest(List.of(LlmMessage.system("consigne systeme sensible"),
                LlmMessage.user(PROMPT_SECRET)), null));
        repondre(500, PROMPT_SECRET); // le fournisseur echoe le prompt dans l'erreur
        client.complete(LlmRequest.of(PROMPT_SECRET));

        for (ILoggingEvent event : logs.list) {
            String rendu = event.getFormattedMessage();
            assertThat(rendu).doesNotContain(PROMPT_SECRET);
            assertThat(rendu).doesNotContain("REPONSE-MODELE-CONFIDENTIELLE");
            assertThat(rendu).doesNotContain("consigne systeme sensible");
        }
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    void complete_filtre_et_aligne_la_fenetre_des_12_derniers_messages() {
        List<LlmMessage> messages = historiqueTermineParAssistant();
        List<String> attendus = contenusDe(6, 16);

        assertPayload(false, new LlmRequest(messages, 0.7), attendus);
    }

    @Test
    void stream_filtre_et_aligne_la_fenetre_des_12_derniers_messages() {
        List<LlmMessage> messages = historiqueTermineParAssistant();
        List<String> attendus = contenusDe(6, 16);

        assertPayload(true, new LlmRequest(messages, 0.7), attendus);
    }

    @Test
    void complete_conserve_12_messages_quand_la_fenetre_commence_par_user() {
        List<LlmMessage> messages = historiqueCommencantParUserDansLaFenetre();

        assertPayload(false, new LlmRequest(messages, null), contenusDe(2, 13));
    }

    @Test
    void stream_conserve_12_messages_quand_la_fenetre_commence_par_user() {
        List<LlmMessage> messages = historiqueCommencantParUserDansLaFenetre();

        assertPayload(true, new LlmRequest(messages, null), contenusDe(2, 13));
    }

    // ────────────────────────────────────────────────────────────────
    // Stub HTTP
    // ────────────────────────────────────────────────────────────────

    private void repondre200(String json) {
        repondre(200, json);
    }

    private void repondre(int statut, String corps) {
        remplacerHandler(exchange -> {
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = corps.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statut, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private void repondreSse(List<String> lignes) {
        remplacerHandler(exchange -> {
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                for (String ligne : lignes) {
                    os.write((ligne + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
        });
    }

    private void assertSystemPromptFirst() {
        try {
            JsonNode messages = new ObjectMapper().readTree(lastRequestBody.get()).path("messages");
            assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
            assertThat(messages.get(0).path("content").asText()).isEqualTo(SYSTEM_PROMPT);
            assertThat(messages).hasSize(2);
            assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
            assertThat(messages.get(1).path("content").asText()).isEqualTo(PROMPT_SECRET);
        } catch (Exception e) {
            throw new AssertionError("La requete LLM doit contenir un tableau de messages valide", e);
        }
    }

    private void assertPayload(boolean stream, LlmRequest request, List<String> expectedContents) {
        if (stream) {
            repondreSse(List.of("data: [DONE]"));
            client.stream(request, ignored -> { });
        } else {
            repondre200("""
                    {"choices":[{"message":{"content":"ok"},"finish_reason":"stop"}]}""");
            client.complete(request);
        }

        try {
            JsonNode root = new ObjectMapper().readTree(lastRequestBody.get());
            JsonNode sentMessages = root.path("messages");
            assertThat(root.path("model").asText()).isEqualTo("modele-test");
            assertThat(root.path("max_tokens").asInt()).isEqualTo(256);
            assertThat(root.path("temperature").asDouble()).isEqualTo(request.temperature() == null ? 0.3 : 0.7);
            assertThat(sentMessages).hasSize(expectedContents.size() + 1);
            assertThat(sentMessages.get(0).path("role").asText()).isEqualTo("system");
            assertThat(sentMessages.get(0).path("content").asText()).isEqualTo(SYSTEM_PROMPT);
            for (int i = 0; i < expectedContents.size(); i++) {
                JsonNode message = sentMessages.get(i + 1);
                String content = expectedContents.get(i);
                assertThat(message.path("content").asText()).isEqualTo(content);
                String expectedRole = request.messages().stream()
                        .filter(candidate -> candidate.content().equals(content))
                        .findFirst()
                        .orElseThrow()
                        .role();
                assertThat(message.path("role").asText()).isEqualTo(expectedRole);
            }
            assertThat(sentMessages.get(sentMessages.size() - 1).path("role").asText()).isEqualTo("user");
        } catch (Exception e) {
            throw new AssertionError("Le payload LLM doit contenir une requete JSON valide", e);
        }
    }

    private static List<LlmMessage> historiqueTermineParAssistant() {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system("system prompt fourni par le client"));
        for (int i = 0; i <= 16; i++) {
            messages.add(i % 2 == 0 ? LlmMessage.user("message-" + i) : LlmMessage.assistant("message-" + i));
        }
        messages.add(LlmMessage.assistant("assistant-apres-user"));
        messages.add(5, LlmMessage.system("autre system prompt client"));
        return messages;
    }

    private static List<LlmMessage> historiqueCommencantParUserDansLaFenetre() {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.user("message-0"));
        messages.add(LlmMessage.assistant("message-1"));
        for (int i = 2; i <= 13; i++) {
            boolean user = i == 2 || i == 4 || i == 6 || i == 8 || i == 10 || i == 13;
            messages.add(user ? LlmMessage.user("message-" + i) : LlmMessage.assistant("message-" + i));
        }
        messages.add(3, LlmMessage.system("system prompt client"));
        return messages;
    }

    private static List<String> contenusDe(int premier, int dernier) {
        List<String> contenus = new ArrayList<>();
        for (int i = premier; i <= dernier; i++) {
            contenus.add("message-" + i);
        }
        return contenus;
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws Exception;
    }

    private void remplacerHandler(ExchangeHandler handler) {
        try {
            server.removeContext("/v1/chat/completions");
        } catch (IllegalArgumentException ignore) {
            // Aucun handler enregistre pour l'instant.
        }
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        });
    }
}
