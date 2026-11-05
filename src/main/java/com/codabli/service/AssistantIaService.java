package com.codabli.service;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.ConteContextData;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AssistantIaService {

    private static final Logger log = LoggerFactory.getLogger(AssistantIaService.class);

    private final LlmClient llmClient;
    private final AssistantPromptModerationService promptModerationService;
    private final MessageAssistantIaService messageService;
    private final PromptAnonymizerService promptAnonymizerService;
    private final ConteContextService conteContextService;
    private final ConteContextFormatter conteContextFormatter;
    private final LlmSystemPromptService systemPromptService;

    public AssistantIaService(LlmClient llmClient,
                              AssistantPromptModerationService promptModerationService,
                              MessageAssistantIaService messageService,
                              PromptAnonymizerService promptAnonymizerService,
                              ConteContextService conteContextService,
                              ConteContextFormatter conteContextFormatter,
                              LlmSystemPromptService systemPromptService) {
        this.llmClient = llmClient;
        this.promptModerationService = promptModerationService;
        this.messageService = messageService;
        this.promptAnonymizerService = promptAnonymizerService;
        this.conteContextService = conteContextService;
        this.conteContextFormatter = conteContextFormatter;
        this.systemPromptService = systemPromptService;
    }

    public List<MessageIaDto> listerHistorique(UUID conteDanseId, Jwt jwt) {
        return messageService.listerHistorique(conteDanseId, jwt);
    }

    public ConversationPreparee preparerConversation(UUID conteDanseId,
                                                     AssistantIaRequest request,
                                                     Jwt jwt) {
        List<MessageIaDto> historique = messageService.listerHistoriquePourConversation(conteDanseId, jwt);
        ConteContextData contexteConte = conteContextService.charger(conteDanseId, jwt);
        List<LlmMessage> messages = new ArrayList<>(historique.size() + 1);

        String nouveauMessage = promptAnonymizerService.anonymize(request.contenu());
        historique.stream()
                .map(message -> new LlmMessage(message.role().name(), message.contenu()))
                .forEach(messages::add);
        messages.add(LlmMessage.user(nouveauMessage));

        return new ConversationPreparee(conteDanseId, nouveauMessage, jwt,
                new LlmRequest(messages, request.temperature()),
                conteContextFormatter.ajouterAuPrompt(systemPromptService.prompt(), contexteConte));
    }

    public LlmResponse streamerConversation(ConversationPreparee conversation, Consumer<String> onToken) {
        Optional<LlmResponse> moderationResponse =
                promptModerationService.moderer("assistant-ia", conversation.conteDanseId(),
                        conversation.nouveauMessage(), onToken);
        if (moderationResponse.isPresent()) {
            return moderationResponse.orElseThrow();
        }
        LlmResponse response = llmClient.stream(
                conversation.requeteLlm(), conversation.systemPrompt(), onToken);
        if (!response.degrade()) {
            messageService.ajouterTour(conversation.conteDanseId(), conversation.nouveauMessage(),
                    response.texte(), conversation.jwt());
        }
        return response;
    }

    public record ConversationPreparee(
            UUID conteDanseId,
            String nouveauMessage,
            Jwt jwt,
            LlmRequest requeteLlm,
            String systemPrompt) {
    }
}
