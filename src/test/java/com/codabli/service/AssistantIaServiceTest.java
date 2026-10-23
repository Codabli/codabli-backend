package com.codabli.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import com.codabli.entity.enums.RoleMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantIaServiceTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private MessageAssistantIaService messageService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private AssistantIaService assistantIaService;

    @Test
    void streamerConversation_shouldStreamResponseAndPersistSuccessfulTurn() {
        UUID conteDanseId = UUID.randomUUID();
        MessageIaDto ancienMessage = new MessageIaDto(UUID.randomUUID(), conteDanseId,
                RoleMessage.user, "Question précédente", Instant.parse("2026-01-01T10:00:00Z"));
        MessageIaDto ancienneReponse = new MessageIaDto(UUID.randomUUID(), conteDanseId,
                RoleMessage.assistant, "Réponse précédente", Instant.parse("2026-01-01T10:00:01Z"));
        when(messageService.listerHistorique(conteDanseId, jwt)).thenReturn(List.of(ancienMessage, ancienneReponse));
        LlmResponse response = LlmResponse.ok("Réponse actuelle", "", "stop", 10, 5);
        AtomicReference<String> fragmentEmis = new AtomicReference<>();
        when(llmClient.stream(any(LlmRequest.class), any())).thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<String>>getArgument(1).accept("Réponse actuelle");
            return response;
        });
        AssistantIaRequest request = new AssistantIaRequest("Nouvelle question", 0.4);

        AssistantIaService.ConversationPreparee conversation =
                assistantIaService.preparerConversation(conteDanseId, request, jwt);
        LlmResponse result = assistantIaService.streamerConversation(conversation, fragmentEmis::set);

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).stream(requestCaptor.capture(), any());
        assertEquals(List.of(
                LlmMessage.user("Question précédente"),
                LlmMessage.assistant("Réponse précédente"),
                LlmMessage.user("Nouvelle question")), requestCaptor.getValue().messages());
        assertEquals(0.4, requestCaptor.getValue().temperature());
        assertEquals("Réponse actuelle", fragmentEmis.get());
        assertSame(response, result);
        verify(messageService).ajouterTour(conteDanseId, "Nouvelle question", "Réponse actuelle", jwt);
    }

    @Test
    void streamerConversation_shouldNotPersistFallbackResponse() {
        UUID conteDanseId = UUID.randomUUID();
        when(messageService.listerHistorique(conteDanseId, jwt)).thenReturn(List.of());
        LlmResponse response = LlmResponse.standby("Assistant indisponible");
        when(llmClient.stream(any(LlmRequest.class), any())).thenReturn(response);

        AssistantIaService.ConversationPreparee conversation = assistantIaService.preparerConversation(
                conteDanseId, new AssistantIaRequest("Bonjour", null), jwt);
        LlmResponse result = assistantIaService.streamerConversation(conversation, ignored -> { });

        assertSame(response, result);
        verify(messageService, never()).ajouterTour(eq(conteDanseId), any(), any(), eq(jwt));
    }
}
