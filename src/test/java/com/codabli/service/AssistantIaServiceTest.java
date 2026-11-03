package com.codabli.service;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.ConteContextData;
import com.codabli.dto.LlmMessage;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantIaServiceTest {

    private static final String MESSAGE_UNSAFE =
            "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.";
    private static final String MESSAGE_CONTROVERSIAL =
            "Merci de reformuler votre demande en évitant les sujets sensibles.";
    private static final String MESSAGE_QWEN_GUARD_INDISPONIBLE =
            "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.";

    @Mock
    private LlmClient llmClient;

    @Mock
    private AssistantPromptModerationService promptModerationService;

    @Mock
    private MessageAssistantIaService messageService;

    @Mock
    private PromptAnonymizerService promptAnonymizerService;

    @Mock
    private ConteContextService conteContextService;

    @Mock
    private ConteContextFormatter conteContextFormatter;

    @Mock
    private LlmSystemPromptService systemPromptService;

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
        when(messageService.listerHistoriquePourConversation(conteDanseId, jwt))
                .thenReturn(List.of(ancienMessage, ancienneReponse));
        when(promptAnonymizerService.anonymize("Nouvelle question")).thenReturn("Nouvelle question");
        when(promptModerationService.moderer(eq("assistant-ia"), eq(conteDanseId),
                eq("Nouvelle question"), any()))
                .thenReturn(Optional.empty());
        LlmResponse response = LlmResponse.ok("Réponse actuelle", "", "stop", 10, 5);
        AtomicReference<String> fragmentEmis = new AtomicReference<>();
        when(llmClient.stream(any(LlmRequest.class), anyString(), any())).thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<String>>getArgument(2).accept("Réponse actuelle");
            return response;
        });
        stubConteContext(conteDanseId);
        when(systemPromptService.prompt()).thenReturn("Prompt système");
        when(conteContextFormatter.ajouterAuPrompt(anyString(), any())).thenReturn("Prompt système contextualisé");
        AssistantIaRequest request = new AssistantIaRequest("Nouvelle question", 0.4);

        AssistantIaService.ConversationPreparee conversation =
                assistantIaService.preparerConversation(conteDanseId, request, jwt);
        LlmResponse result = assistantIaService.streamerConversation(conversation, fragmentEmis::set);

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).stream(requestCaptor.capture(), eq("Prompt système contextualisé"), any());
        assertEquals(List.of(
                LlmMessage.user("Question précédente"),
                LlmMessage.assistant("Réponse précédente"),
                LlmMessage.user("Nouvelle question")), requestCaptor.getValue().messages());
        assertEquals(0.4, requestCaptor.getValue().temperature());
        assertEquals("Réponse actuelle", fragmentEmis.get());
        assertSame(response, result);
        verify(promptModerationService).moderer(eq("assistant-ia"), eq(conteDanseId),
                eq("Nouvelle question"), any());
        verify(messageService).ajouterTour(conteDanseId, "Nouvelle question", "Réponse actuelle", jwt);
    }

    @Test
    void streamerConversation_shouldNotPersistFallbackResponse() {
        UUID conteDanseId = UUID.randomUUID();
        when(messageService.listerHistoriquePourConversation(conteDanseId, jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Bonjour")).thenReturn("Bonjour");
        when(promptModerationService.moderer(eq("assistant-ia"), eq(conteDanseId), eq("Bonjour"), any()))
                .thenReturn(Optional.empty());
        LlmResponse response = LlmResponse.standby("Assistant indisponible");
        when(llmClient.stream(any(LlmRequest.class), anyString(), any())).thenReturn(response);
        stubConteContext(conteDanseId);
        when(systemPromptService.prompt()).thenReturn("Prompt système");
        when(conteContextFormatter.ajouterAuPrompt(anyString(), any())).thenReturn("Prompt système contextualisé");

        AssistantIaService.ConversationPreparee conversation = assistantIaService.preparerConversation(
                conteDanseId, new AssistantIaRequest("Bonjour", null), jwt);
        LlmResponse result = assistantIaService.streamerConversation(conversation, ignored -> {
        });

        assertSame(response, result);
        verify(messageService, never()).ajouterTour(eq(conteDanseId), any(), any(), eq(jwt));
    }

    @Test
    void streamerConversation_shouldRejectUnsafePromptWithoutCallingLlm() {
        UUID conteDanseId = UUID.randomUUID();
        AssistantIaService.ConversationPreparee conversation = conversation(conteDanseId, "prompt anonymise");
        stubModerationResponse(conteDanseId, "prompt anonymise", MESSAGE_UNSAFE, false);
        AtomicReference<String> emittedMessage = new AtomicReference<>();

        LlmResponse response = assistantIaService.streamerConversation(conversation, emittedMessage::set);

        assertEquals(MESSAGE_UNSAFE, emittedMessage.get());
        assertEquals(MESSAGE_UNSAFE, response.texte());
        verifyNoInteractions(llmClient);
        verify(promptModerationService).moderer(eq("assistant-ia"), eq(conteDanseId),
                eq("prompt anonymise"), any());
        verify(messageService, never()).ajouterTour(eq(conteDanseId), any(), any(), eq(jwt));
    }

    @Test
    void streamerConversation_shouldRejectControversialPromptWithoutCallingLlm() {
        UUID conteDanseId = UUID.randomUUID();
        AssistantIaService.ConversationPreparee conversation = conversation(conteDanseId, "prompt anonymise");
        stubModerationResponse(conteDanseId, "prompt anonymise", MESSAGE_CONTROVERSIAL, false);
        AtomicReference<String> emittedMessage = new AtomicReference<>();

        LlmResponse response = assistantIaService.streamerConversation(conversation, emittedMessage::set);

        assertEquals(MESSAGE_CONTROVERSIAL, emittedMessage.get());
        assertEquals(MESSAGE_CONTROVERSIAL, response.texte());
        verifyNoInteractions(llmClient);
        verify(messageService, never()).ajouterTour(eq(conteDanseId), any(), any(), eq(jwt));
    }

    @Test
    void streamerConversation_shouldStopWhenQwenGuardIsUnavailable() {
        UUID conteDanseId = UUID.randomUUID();
        AssistantIaService.ConversationPreparee conversation = conversation(conteDanseId, "prompt anonymise");
        stubModerationResponse(conteDanseId, "prompt anonymise", MESSAGE_QWEN_GUARD_INDISPONIBLE, true);
        AtomicReference<String> emittedMessage = new AtomicReference<>();

        LlmResponse response = assistantIaService.streamerConversation(conversation, emittedMessage::set);

        assertEquals(MESSAGE_QWEN_GUARD_INDISPONIBLE, emittedMessage.get());
        assertEquals(MESSAGE_QWEN_GUARD_INDISPONIBLE, response.texte());
        assertTrue(response.degrade());
        verifyNoInteractions(llmClient, messageService);
    }

    @Test
    void preparerConversation_shouldAnonymizePromptBeforeSendingItToLlm() {
        UUID conteDanseId = UUID.randomUUID();
        when(messageService.listerHistoriquePourConversation(conteDanseId, jwt)).thenReturn(List.of());
        stubConteContext(conteDanseId);
        when(systemPromptService.prompt()).thenReturn("Prompt système");
        when(conteContextFormatter.ajouterAuPrompt(anyString(), any())).thenReturn("Prompt système contextualisé");
        when(promptAnonymizerService.anonymize("Écris-moi à enfant@example.com"))
                .thenReturn("Écris-moi à [EMAIL]");

        AssistantIaService.ConversationPreparee conversation = assistantIaService.preparerConversation(
                conteDanseId, new AssistantIaRequest("Écris-moi à enfant@example.com", null), jwt);

        assertEquals("Écris-moi à [EMAIL]", conversation.nouveauMessage());
        assertEquals(List.of(LlmMessage.user("Écris-moi à [EMAIL]")), conversation.requeteLlm().messages());
    }

    @Test
    void preparerConversation_withPublishedConte_shouldRejectBeforeCallingLlm() {
        UUID conteDanseId = UUID.randomUUID();
        when(messageService.listerHistoriquePourConversation(conteDanseId, jwt))
                .thenThrow(new BusinessException(ErrorCode.CONTE_DEJA_PUBLIE, "Conte publie"));

        assertThrows(BusinessException.class, () -> assistantIaService.preparerConversation(
                conteDanseId, new AssistantIaRequest("Bonjour", null), jwt));

        verifyNoInteractions(llmClient, promptAnonymizerService);
    }

    private AssistantIaService.ConversationPreparee conversation(UUID conteDanseId, String prompt) {
        return new AssistantIaService.ConversationPreparee(
                conteDanseId, prompt, jwt, LlmRequest.of(prompt), "Prompt système contextualisé");
    }

    private void stubConteContext(UUID conteDanseId) {
        when(conteContextService.charger(conteDanseId, jwt))
                .thenReturn(new ConteContextData("Titre", "Thématique", List.of(), "Pays", "Culture", 4, 12));
    }

    private void stubModerationResponse(UUID conteDanseId, String prompt, String message, boolean degrade) {
        LlmResponse response = degrade
                ? LlmResponse.standby(message)
                : LlmResponse.ok(message, "", "stop", null, null);
        when(promptModerationService.moderer(eq("assistant-ia"), eq(conteDanseId), eq(prompt), any()))
                .thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<String>>getArgument(3).accept(message);
            return Optional.of(response);
        });
    }
}
