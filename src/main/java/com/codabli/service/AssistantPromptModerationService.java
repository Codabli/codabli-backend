package com.codabli.service;

import com.codabli.dto.LlmResponse;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AssistantPromptModerationService {

    private static final Logger log = LoggerFactory.getLogger(AssistantPromptModerationService.class);
    private static final String MESSAGE_UNSAFE =
            "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.";
    private static final String MESSAGE_CONTROVERSIAL =
            "Merci de reformuler votre demande en évitant les sujets sensibles.";
    private static final String MESSAGE_QWEN_GUARD_INDISPONIBLE =
            "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.";

    private final QwenGuardClient qwenGuardClient;
    private final RefusModerationQwenGuardService refusService;

    public AssistantPromptModerationService(QwenGuardClient qwenGuardClient,
                                            RefusModerationQwenGuardService refusService) {
        this.qwenGuardClient = qwenGuardClient;
        this.refusService = refusService;
    }

    public Optional<LlmResponse> moderer(String assistant,
                                         UUID conteDanseId,
                                         String prompt,
                                         Consumer<String> onToken) {
        SafetyAnalysis safetyAnalysis;
        try {
            safetyAnalysis = qwenGuardClient.analyser(prompt);
        } catch (QwenGuardIndisponibleException e) {
            log.warn("QwenGuard indisponible pour {}: {}", assistant, e.getMessage());
            return Optional.of(envoyerControleIndisponible(onToken));
        }

        if (safetyAnalysis == null || safetyAnalysis.status() == null) {
            log.warn("QwenGuard sans statut exploitable pour {}", assistant);
            return Optional.of(envoyerControleIndisponible(onToken));
        }

        String details = safetyAnalysis.details() == null
                ? "" : safetyAnalysis.details().replaceAll("[\\r\\n]+", " | ");
        log.info("QwenGuard pour {}: statut={}, details={}", assistant, safetyAnalysis.status(), details);
        return switch (safetyAnalysis.status()) {
            case SAFE -> Optional.empty();
            case UNSAFE -> Optional.of(refuser(assistant, conteDanseId, prompt, safetyAnalysis,
                    MESSAGE_UNSAFE, onToken));
            case CONTROVERSIAL -> Optional.of(refuser(assistant, conteDanseId, prompt, safetyAnalysis,
                    MESSAGE_CONTROVERSIAL, onToken));
        };
    }

    private LlmResponse refuser(String assistant,
                                UUID conteDanseId,
                                String prompt,
                                SafetyAnalysis safetyAnalysis,
                                String message,
                                Consumer<String> onToken) {
        try {
            refusService.enregistrerRefus(conteDanseId, assistant, prompt, safetyAnalysis);
        } catch (DataAccessException | TransactionException e) {
            log.error("Impossible de persister le refus QwenGuard: assistant={}, conteDanseId={}, statut={}",
                    assistant, conteDanseId, safetyAnalysis.status(), e);
        }
        return envoyerReponse(message, onToken);
    }

    private LlmResponse envoyerControleIndisponible(Consumer<String> onToken) {
        onToken.accept(MESSAGE_QWEN_GUARD_INDISPONIBLE);
        return LlmResponse.standby(MESSAGE_QWEN_GUARD_INDISPONIBLE);
    }

    private LlmResponse envoyerReponse(String texte, Consumer<String> onToken) {
        onToken.accept(texte);
        return LlmResponse.ok(texte, "", "stop", null, null);
    }
}
