package com.codabli.controller;

import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.service.LlmClient;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

/**
 * Point d'entree HTTP de l'assistant IA souverain (EA-107).
 * <p>
 * Securite : ces routes ne figurent pas dans la liste publique de
 * {@code SecurityConfig}, elles sont donc {@code authenticated()} par defaut.
 */
@RestController
public class AssistantIaController {

    private static final Logger log = LoggerFactory.getLogger(AssistantIaController.class);

    private final LlmClient llmClient;
    private final ExecutorService llmExecutor;

    public AssistantIaController(LlmClient llmClient, ExecutorService llmExecutor) {
        this.llmClient = llmClient;
        this.llmExecutor = llmExecutor;
    }

    /**
     * POST /api/ia/completion — reponse complete (tout d'un coup).
     */
    @PostMapping("/api/ia/completion")
    public ResponseEntity<LlmResponse> completion(@Valid @RequestBody LlmRequest request) {
        return ResponseEntity.ok(llmClient.complete(request));
    }

    /**
     * POST /api/ia/stream — reponse en streaming (au fur et a mesure), via SSE.
     * Chaque fragment de texte est pousse dans un evenement {@code token}.
     */
    @PostMapping(value = "/api/ia/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody LlmRequest request) {
        // Marge sur le timeout du client LLM pour que la coupure vienne du client,
        // pas du serveur HTTP.
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());

        llmExecutor.execute(() -> {
            try {
                llmClient.stream(request, fragment -> {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(fragment));
                    } catch (IOException e) {
                        // Client deconnecte : on arrete proprement le flux.
                        throw new StreamInterrompuException(e);
                    }
                });
                emitter.complete();
            } catch (StreamInterrompuException e) {
                emitter.complete();
            } catch (RuntimeException e) {
                log.warn("Flux IA interrompu: {}", e.getClass().getSimpleName());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** Signale une deconnexion du client pendant le streaming SSE. */
    private static final class StreamInterrompuException extends RuntimeException {
        StreamInterrompuException(Throwable cause) {
            super(cause);
        }
    }
}
