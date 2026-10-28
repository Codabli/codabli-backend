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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        LlmResponse response = client.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly("Bonjour ", "petit");
        assertThat(response.degrade()).isFalse();
        assertThat(response.texte()).isEqualTo("Bonjour petit");
        assertSystemPromptFirst();
    }

    @Test
    void stream_pousse_le_standby_quand_le_llm_echoue() {
        repondre(503, "service indisponible");

        List<String> fragments = new ArrayList<>();
        LlmResponse response = client.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);

        assertThat(fragments).containsExactly(STANDBY);
        assertThat(response.degrade()).isTrue();
        assertThat(response.texte()).isEqualTo(STANDBY);
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
        LlmResponse response = timeoutClient.stream(LlmRequest.of(PROMPT_SECRET), fragments::add);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(fragments).containsExactly("debut", STANDBY);
        assertThat(response.degrade()).isTrue();
        assertThat(elapsedMillis).isLessThan(2500);
    }

    @Test
    void stream_propage_les_erreurs_du_consommateur_sans_emettre_de_fallback() {
        repondreSse(List.of("data: {\"choices\":[{\"delta\":{\"content\":\"Bonjour\"}}]}", "data: [DONE]"));
        List<String> fragments = new ArrayList<>();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> client.stream(LlmRequest.of(PROMPT_SECRET), fragment -> {
                            fragments.add(fragment);
                            throw new IllegalStateException("client deconnecte");
                        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("client deconnecte");

        assertThat(fragments).containsExactly("Bonjour");
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

    @Test
    @DisplayName("complete prepends the backend system prompt and keeps valid history")
    void complete_shouldPrependSystemPromptAndKeepValidHistory() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.user("question-1"),
                LlmMessage.assistant("reponse-1"),
                LlmMessage.user("question-2"));

        assertCapturedPayload(false, new LlmRequest(history, null), history);
    }

    @Test
    @DisplayName("stream prepends the backend system prompt and keeps valid history")
    void stream_shouldPrependSystemPromptAndKeepValidHistory() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.user("question-1"),
                LlmMessage.assistant("reponse-1"),
                LlmMessage.user("question-2"));

        assertCapturedPayload(true, new LlmRequest(history, null), history);
    }

    @Test
    @DisplayName("complete ignores incoming system messages")
    void complete_shouldIgnoreIncomingSystemMessages() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.system("consigne entrante"),
                LlmMessage.user("question"));

        assertCapturedPayload(false, new LlmRequest(history, null), List.of(LlmMessage.user("question")));
    }

    @Test
    @DisplayName("stream ignores incoming system messages")
    void stream_shouldIgnoreIncomingSystemMessages() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.system("consigne entrante"),
                LlmMessage.user("question"));

        assertCapturedPayload(true, new LlmRequest(history, null), List.of(LlmMessage.user("question")));
    }

    @Test
    @DisplayName("complete applies the sliding window and starts history with a user message")
    void complete_shouldApplySlidingWindowAndEnsureFirstMessageIsUser() throws Exception {
        List<LlmMessage> history = historiqueDe15Messages();
        List<LlmMessage> retained = history.subList(4, 15);

        assertThat(history.get(3).role()).isEqualTo(LlmMessage.ROLE_ASSISTANT);
        assertCapturedPayload(false, new LlmRequest(history, null), retained);
    }

    @Test
    @DisplayName("stream applies the sliding window and starts history with a user message")
    void stream_shouldApplySlidingWindowAndEnsureFirstMessageIsUser() throws Exception {
        List<LlmMessage> history = historiqueDe15Messages();
        List<LlmMessage> retained = history.subList(4, 15);

        assertThat(history.get(3).role()).isEqualTo(LlmMessage.ROLE_ASSISTANT);
        assertCapturedPayload(true, new LlmRequest(history, null), retained);
    }

    @Test
    @DisplayName("complete removes a trailing assistant message")
    void complete_shouldRemoveTrailingAssistantMessage() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.user("question-1"),
                LlmMessage.assistant("reponse-1"),
                LlmMessage.user("question-2"),
                LlmMessage.assistant("reponse finale invalide"));

        assertCapturedPayload(false, new LlmRequest(history, null), history.subList(0, 3));
    }

    @Test
    @DisplayName("stream removes a trailing assistant message")
    void stream_shouldRemoveTrailingAssistantMessage() throws Exception {
        List<LlmMessage> history = List.of(
                LlmMessage.user("question-1"),
                LlmMessage.assistant("reponse-1"),
                LlmMessage.user("question-2"),
                LlmMessage.assistant("reponse finale invalide"));

        assertCapturedPayload(true, new LlmRequest(history, null), history.subList(0, 3));
    }

    // ────────────────────────────────────────────────────────────────
    // Stub HTTP
    // ────────────────────────────────────────────────────────────────

    private void assertCapturedPayload(boolean stream, LlmRequest request, List<LlmMessage> expectedHistory)
            throws Exception {
        HttpClient mockedHttpClient = mock(HttpClient.class);
        Object responseBody = stream
                ? new ByteArrayInputStream("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8))
                : """
                {"choices":[{"message":{"content":"ok"},"finish_reason":"stop"}]}""";
        doReturn(reponseHttp(responseBody)).when(mockedHttpClient).send(any(HttpRequest.class), any());

        OvhOpenAiClient unitClient = new OvhOpenAiClient(mockedHttpClient,
                new LlmProperties("https://ovh.example/v1", "cle-test", "modele-test", SYSTEM_PROMPT, 0.3, 256,
                        Duration.ofSeconds(5), STANDBY));
        if (stream) {
            unitClient.stream(request, ignored -> {
            });
        } else {
            unitClient.complete(request);
        }

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockedHttpClient).send(requestCaptor.capture(), any());
        JsonNode sentMessages = new ObjectMapper().readTree(lireCorps(requestCaptor.getValue())).path("messages");

        assertThat(sentMessages).hasSize(expectedHistory.size() + 1);
        assertThat(sentMessages.get(0).path("role").asText()).isEqualTo(LlmMessage.ROLE_SYSTEM);
        assertThat(sentMessages.get(0).path("content").asText()).isEqualTo(SYSTEM_PROMPT);
        for (int i = 0; i < expectedHistory.size(); i++) {
            JsonNode message = sentMessages.get(i + 1);
            assertThat(message.path("role").asText()).isEqualTo(expectedHistory.get(i).role());
            assertThat(message.path("content").asText()).isEqualTo(expectedHistory.get(i).content());
        }
        assertThat(sentMessages.size()).isLessThanOrEqualTo(12);
        assertThat(sentMessages.get(1).path("role").asText()).isEqualTo(LlmMessage.ROLE_USER);
        assertThat(sentMessages.get(sentMessages.size() - 1).path("role").asText())
                .isEqualTo(LlmMessage.ROLE_USER);
    }

    private static HttpResponse<Object> reponseHttp(Object body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                return HttpRequest.newBuilder(URI.create("https://ovh.example/v1/chat/completions")).build();
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (name, value) -> true);
            }

            @Override
            public Object body() {
                return body;
            }

            @Override
            public Optional<javax.net.ssl.SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create("https://ovh.example/v1/chat/completions");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    private static String lireCorps(HttpRequest request) throws InterruptedException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                body.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                failure.set(throwable);
                completed.countDown();
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        });
        if (!completed.await(1, TimeUnit.SECONDS)) {
            throw new AssertionError("Le corps de la requete HTTP n'a pas ete publie");
        }
        if (failure.get() != null) {
            throw new AssertionError("Impossible de lire le corps de la requete HTTP", failure.get());
        }
        return body.toString(StandardCharsets.UTF_8);
    }

    private static List<LlmMessage> historiqueDe15Messages() {
        List<LlmMessage> messages = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            messages.add(i % 2 == 0
                    ? LlmMessage.user("message-" + i)
                    : LlmMessage.assistant("message-" + i));
        }
        return messages;
    }

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
            client.stream(request, ignored -> {
            });
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
