package com.codabli.service;

import com.codabli.entity.RefusModerationQwenGuard;
import com.codabli.repository.RefusModerationQwenGuardRepository;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefusModerationQwenGuardServiceTest {

    private final RefusModerationQwenGuardRepository repository =
            mock(RefusModerationQwenGuardRepository.class);
    private final RefusModerationQwenGuardService service =
            new RefusModerationQwenGuardService(repository);

    @Test
    void enregistrerRefus_shouldPersistPromptAndQwenGuardResponse() {
        UUID conteDanseId = UUID.randomUUID();
        String prompt = "prompt anonymisé";
        SafetyAnalysis analyse = new SafetyAnalysis(SafetyStatus.UNSAFE, "Safety: Unsafe");
        when(repository.save(any(RefusModerationQwenGuard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enregistrerRefus(conteDanseId, "assistant-ia", prompt, analyse);

        ArgumentCaptor<RefusModerationQwenGuard> captor =
                ArgumentCaptor.forClass(RefusModerationQwenGuard.class);
        verify(repository).save(captor.capture());
        RefusModerationQwenGuard refus = captor.getValue();
        assertEquals(conteDanseId, refus.getConteDanseId());
        assertEquals("assistant-ia", refus.getAssistant());
        assertEquals(SafetyStatus.UNSAFE, refus.getStatut());
        assertEquals(prompt, refus.getPromptInitial());
        assertEquals("Safety: Unsafe", refus.getRetourQwenGuard());
    }

    @Test
    void enregistrerRefus_shouldRejectSafeAnalysis() {
        SafetyAnalysis analyse = new SafetyAnalysis(SafetyStatus.SAFE, "Safety: Safe");

        assertThrows(IllegalArgumentException.class,
                () -> service.enregistrerRefus(UUID.randomUUID(), "assistant-ia", "prompt", analyse));
    }
}
