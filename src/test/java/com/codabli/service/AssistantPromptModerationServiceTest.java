package com.codabli.service;

import com.codabli.dto.LlmResponse;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantPromptModerationServiceTest {

    private static final UUID CONTE_DANSE_ID = UUID.randomUUID();
    private static final String PROMPT = "prompt anonymisé";
    private static final String MESSAGE_UNSAFE =
            "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.";
    private static final String MESSAGE_CONTROVERSIAL =
            "Merci de reformuler votre demande en évitant les sujets sensibles.";
    private static final String MESSAGE_UNAVAILABLE =
            "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.";

    private final QwenGuardClient qwenGuardClient = mock(QwenGuardClient.class);
    private final RefusModerationQwenGuardService refusService = mock(RefusModerationQwenGuardService.class);
    private final AssistantPromptModerationService moderationService =
            new AssistantPromptModerationService(qwenGuardClient, refusService);

    @BeforeEach
    void setUp() {
        when(qwenGuardClient.analyser(PROMPT))
                .thenReturn(new SafetyAnalysis(SafetyStatus.SAFE, "Safety: Safe"));
    }

    @Test
    void moderer_shouldReturnEmptyForSafePrompt() {
        Optional<LlmResponse> result =
                moderationService.moderer("assistant-ia", CONTE_DANSE_ID, PROMPT, ignored -> {});

        assertTrue(result.isEmpty());
        verify(refusService, never()).enregistrerRefus(any(), any(), any(), any());
    }

    @Test
    void moderer_shouldRejectUnsafePromptAndEmitRefusal() {
        AtomicReference<String> emittedToken = new AtomicReference<>();
        SafetyAnalysis analyse = new SafetyAnalysis(SafetyStatus.UNSAFE, "Safety: Unsafe");
        when(qwenGuardClient.analyser(PROMPT)).thenReturn(analyse);

        Optional<LlmResponse> result =
                moderationService.moderer("assistant-ia", CONTE_DANSE_ID, PROMPT, emittedToken::set);

        assertTrue(result.isPresent());
        assertEquals(MESSAGE_UNSAFE, emittedToken.get());
        assertEquals(MESSAGE_UNSAFE, result.orElseThrow().texte());
        assertFalse(result.orElseThrow().degrade());
        verify(refusService).enregistrerRefus(CONTE_DANSE_ID, "assistant-ia", PROMPT, analyse);
    }

    @Test
    void moderer_shouldRejectControversialPromptAndEmitRefusal() {
        AtomicReference<String> emittedToken = new AtomicReference<>();
        SafetyAnalysis analyse = new SafetyAnalysis(SafetyStatus.CONTROVERSIAL, "Safety: Controversial");
        when(qwenGuardClient.analyser(PROMPT)).thenReturn(analyse);
        Optional<LlmResponse> result = moderationService.moderer(
                "assistant-creation-univers", CONTE_DANSE_ID, PROMPT, emittedToken::set);

        assertTrue(result.isPresent());
        assertEquals(MESSAGE_CONTROVERSIAL, emittedToken.get());
        assertEquals(MESSAGE_CONTROVERSIAL, result.orElseThrow().texte());
        assertFalse(result.orElseThrow().degrade());
        verify(refusService).enregistrerRefus(
                CONTE_DANSE_ID, "assistant-creation-univers", PROMPT, analyse);
    }

    @Test
    void moderer_shouldReturnStandbyWhenQwenGuardIsUnavailable() {
        QwenGuardIndisponibleException exception =
                new QwenGuardIndisponibleException("QwenGuard indisponible");
        when(qwenGuardClient.analyser(PROMPT)).thenThrow(exception);
        AtomicReference<String> emittedToken = new AtomicReference<>();

        Optional<LlmResponse> result =
                moderationService.moderer("assistant-ia", CONTE_DANSE_ID, PROMPT, emittedToken::set);

        assertTrue(result.isPresent());
        assertEquals(MESSAGE_UNAVAILABLE, emittedToken.get());
        assertEquals(MESSAGE_UNAVAILABLE, result.orElseThrow().texte());
        assertTrue(result.orElseThrow().degrade());
        verify(refusService, never()).enregistrerRefus(any(), any(), any(), any());
    }

    @Test
    void moderer_shouldReturnStandbyWhenQwenGuardReturnsNoUsableStatus() {
        when(qwenGuardClient.analyser(PROMPT)).thenReturn(null);

        Optional<LlmResponse> result =
                moderationService.moderer("assistant-ia", CONTE_DANSE_ID, PROMPT, ignored -> {});

        assertTrue(result.isPresent());
        assertEquals(MESSAGE_UNAVAILABLE, result.orElseThrow().texte());
        assertTrue(result.orElseThrow().degrade());
        verify(refusService, never()).enregistrerRefus(any(), any(), any(), any());
    }

    @Test
    void moderer_shouldStillRejectWhenRefusalPersistenceFails() {
        when(qwenGuardClient.analyser(PROMPT))
                .thenReturn(new SafetyAnalysis(SafetyStatus.UNSAFE, "Safety: Unsafe"));
        when(refusService.enregistrerRefus(eq(CONTE_DANSE_ID), eq("assistant-ia"), eq(PROMPT), any()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        AtomicReference<String> emittedToken = new AtomicReference<>();

        Optional<LlmResponse> result =
                moderationService.moderer("assistant-ia", CONTE_DANSE_ID, PROMPT, emittedToken::set);

        assertTrue(result.isPresent());
        assertEquals(MESSAGE_UNSAFE, emittedToken.get());
        assertEquals(MESSAGE_UNSAFE, result.orElseThrow().texte());
        assertFalse(result.orElseThrow().degrade());
    }
}
