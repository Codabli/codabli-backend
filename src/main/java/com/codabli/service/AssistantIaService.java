package com.codabli.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AssistantIaService {

    private final LlmClient llmClient;
    private final MessageAssistantIaService messageService;

    public AssistantIaService(LlmClient llmClient, MessageAssistantIaService messageService) {
        this.llmClient = llmClient;
        this.messageService = messageService;
    }

    public List<MessageIaDto> listerHistorique(UUID conteDanseId, Jwt jwt) {
        return messageService.listerHistorique(conteDanseId, jwt);
    }

    public ConversationPreparee preparerConversation(UUID conteDanseId,
                                                     AssistantIaRequest request,
                                                     Jwt jwt) {
        List<MessageIaDto> historique = messageService.listerHistorique(conteDanseId, jwt);
        List<LlmMessage> messages = new ArrayList<>(historique.size() + 1);
        historique.stream()
                .map(message -> new LlmMessage(message.role().name(), message.contenu()))
                .forEach(messages::add);
        messages.add(LlmMessage.user(request.contenu()));

        return new ConversationPreparee(conteDanseId, request.contenu(), jwt,
                new LlmRequest(messages, request.temperature()));
    }

    public LlmResponse streamerConversation(ConversationPreparee conversation, Consumer<String> onToken) {
        LlmResponse response = llmClient.stream(conversation.requeteLlm(), onToken);
        if (!response.degrade()) {
            messageService.ajouterTour(conversation.conteDanseId(), conversation.nouveauMessage(),
                    response.texte(), conversation.jwt());
        }
        return response;
    }

    public record ConversationPreparee(UUID conteDanseId, String nouveauMessage, Jwt jwt, LlmRequest requeteLlm) {
    }
}
