package com.codabli.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codabli.config.LlmProperties;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
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
    private static final String STANDBY = "Message de repli enfant";

    private HttpServer server;
    private OvhOpenAiClient client;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1";
        LlmProperties props = new LlmProperties(
                baseUrl, "cle-test", "modele-test", 0.3, 256,
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

        LlmResponse response = client.complete(LlmRequest.of("systeme", PROMPT_SECRET));

        assertThat(response.degrade()).isFalse();
        assertThat(response.texte()).isEqualTo("Il etait une fois...");
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.tokensIn()).isEqualTo(12);
        assertThat(response.tokensOut()).isEqualTo(5);
    }

    @Test
    void complete_renvoie_le_standby_quand_le_llm_echoue() {
        repondre(500, "erreur interne cote fournisseur");

        LlmResponse response = client.complete(LlmRequest.of("systeme", PROMPT_SECRET));

        assertThat(response.degrade()).isTrue();
        assertThat(response.texte()).isEqualTo(STANDBY);
    }

    @Test
    void stream_pousse_les_fragments_au_fur_et_a_mesure() {
        repondreSse(List.of(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Bonjour \"}}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"petit\"}}]}",
                "data: [DONE]"));

        List<String> fragments = new ArrayList<>();
        client.stream(LlmRequest.of("systeme", PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly("Bonjour ", "petit");
    }

    @Test
    void stream_pousse_le_standby_quand_le_llm_echoue() {
        repondre(503, "service indisponible");

        List<String> fragments = new ArrayList<>();
        client.stream(LlmRequest.of("systeme", PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly(STANDBY);
    }

    @Test
    void aucun_contenu_de_prompt_ni_de_reponse_dans_les_logs() {
        repondre200("""
                {"choices":[{"message":{"content":"REPONSE-MODELE-CONFIDENTIELLE"},
                 "finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1}}""");

        client.complete(LlmRequest.of("consigne systeme sensible", PROMPT_SECRET));
        repondre(500, PROMPT_SECRET); // le fournisseur echoe le prompt dans l'erreur
        client.complete(LlmRequest.of("systeme", PROMPT_SECRET));

        for (ILoggingEvent event : logs.list) {
            String rendu = event.getFormattedMessage();
            assertThat(rendu).doesNotContain(PROMPT_SECRET);
            assertThat(rendu).doesNotContain("REPONSE-MODELE-CONFIDENTIELLE");
            assertThat(rendu).doesNotContain("consigne systeme sensible");
        }
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getLevel()).isEqualTo(Level.WARN));
    }

    // ────────────────────────────────────────────────────────────────
    // Stub HTTP
    // ────────────────────────────────────────────────────────────────

    private void repondre200(String json) {
        repondre(200, json);
    }

    private void repondre(int statut, String corps) {
        remplacerHandler(exchange -> {
            byte[] bytes = corps.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statut, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private void repondreSse(List<String> lignes) {
        remplacerHandler(exchange -> {
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
