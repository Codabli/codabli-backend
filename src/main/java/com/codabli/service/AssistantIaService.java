package com.codabli.service;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AssistantIaService {

    private static final Logger log = LoggerFactory.getLogger(AssistantIaService.class);
    private static final String MESSAGE_UNSAFE =
            "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.";
    private static final String MESSAGE_CONTROVERSIAL =
            "Merci de reformuler votre demande en évitant les sujets sensibles.";
    private static final String MESSAGE_QWEN_GUARD_INDISPONIBLE =
            "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.";

    private final LlmClient llmClient;
    private final QwenGuardClient qwenGuardClient;
    private final MessageAssistantIaService messageService;
    private final PromptAnonymizerService promptAnonymizerService;

    public AssistantIaService(LlmClient llmClient,
                              QwenGuardClient qwenGuardClient,
                              MessageAssistantIaService messageService,
                              PromptAnonymizerService promptAnonymizerService) {
        this.llmClient = llmClient;
        this.qwenGuardClient = qwenGuardClient;
        this.messageService = messageService;
        this.promptAnonymizerService = promptAnonymizerService;
    }

    public List<MessageIaDto> listerHistorique(UUID conteDanseId, Jwt jwt) {
        return messageService.listerHistorique(conteDanseId, jwt);
    }

    public ConversationPreparee preparerConversation(UUID conteDanseId,
                                                     AssistantIaRequest request,
                                                     Jwt jwt) {
        List<MessageIaDto> historique = messageService.listerHistoriquePourConversation(conteDanseId, jwt);
        List<LlmMessage> messages = new ArrayList<>(historique.size() + 1);

        String nouveauMessage = promptAnonymizerService.anonymize(request.contenu());
        historique.stream()
                .map(message -> new LlmMessage(message.role().name(), message.contenu()))
                .forEach(messages::add);
        messages.add(LlmMessage.user(nouveauMessage));

        return new ConversationPreparee(conteDanseId, nouveauMessage, jwt,
                new LlmRequest(messages, request.temperature()));
    }

    public LlmResponse streamerConversation(ConversationPreparee conversation, Consumer<String> onToken) {
        SafetyAnalysis safetyAnalysis;
        try {
            safetyAnalysis = qwenGuardClient.analyser(conversation.nouveauMessage());
        } catch (QwenGuardIndisponibleException e) {
            log.warn("QwenGuard indisponible pour assistant-ia: {}", e.getMessage());
            return envoyerControleIndisponible(onToken);
        }
        if (safetyAnalysis == null || safetyAnalysis.status() == null) {
            log.warn("QwenGuard sans statut exploitable pour assistant-ia");
            return envoyerControleIndisponible(onToken);
        }
        String details = safetyAnalysis.details() == null
                ? "" : safetyAnalysis.details().replaceAll("[\\r\\n]+", " | ");
        log.info("QwenGuard pour assistant-ia: statut={}, details={}", safetyAnalysis.status(), details);
        LlmResponse response = switch (safetyAnalysis.status()) {
            case SAFE -> llmClient.stream(conversation.requeteLlm(), onToken);
            case UNSAFE -> envoyerReponse(MESSAGE_UNSAFE, onToken);
            case CONTROVERSIAL -> envoyerReponse(MESSAGE_CONTROVERSIAL, onToken);
        };
        if (!response.degrade()) {
            messageService.ajouterTour(conversation.conteDanseId(), conversation.nouveauMessage(),
                    response.texte(), conversation.jwt());
        }
        return response;
    }

    private LlmResponse envoyerControleIndisponible(Consumer<String> onToken) {
        onToken.accept(MESSAGE_QWEN_GUARD_INDISPONIBLE);
        return LlmResponse.standby(MESSAGE_QWEN_GUARD_INDISPONIBLE);
    }

    private LlmResponse envoyerReponse(String texte, Consumer<String> onToken) {
        onToken.accept(texte);
        return LlmResponse.ok(texte, "", "stop", null, null);
    }

    public record ConversationPreparee(UUID conteDanseId, String nouveauMessage, Jwt jwt, LlmRequest requeteLlm) {
    }
}
