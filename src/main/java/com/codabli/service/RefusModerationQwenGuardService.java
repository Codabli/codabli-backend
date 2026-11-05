package com.codabli.service;

import com.codabli.entity.RefusModerationQwenGuard;
import com.codabli.repository.RefusModerationQwenGuardRepository;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class RefusModerationQwenGuardService {

    private final RefusModerationQwenGuardRepository repository;

    public RefusModerationQwenGuardService(RefusModerationQwenGuardRepository repository) {
        this.repository = repository;
    }

    public RefusModerationQwenGuard enregistrerRefus(UUID conteDanseId,
                                                     String assistant,
                                                     String promptInitial,
                                                     SafetyAnalysis analyse) {
        Objects.requireNonNull(conteDanseId, "conteDanseId est obligatoire");
        Objects.requireNonNull(assistant, "assistant est obligatoire");
        Objects.requireNonNull(promptInitial, "promptInitial est obligatoire");
        Objects.requireNonNull(analyse, "analyse QwenGuard est obligatoire");
        Objects.requireNonNull(analyse.status(), "statut QwenGuard est obligatoire");
        Objects.requireNonNull(analyse.details(), "retour QwenGuard est obligatoire");

        if (assistant.isBlank()) {
            throw new IllegalArgumentException("assistant ne peut pas etre vide");
        }
        if (analyse.status() == SafetyStatus.SAFE) {
            throw new IllegalArgumentException("Un resultat SAFE ne constitue pas un refus");
        }

        RefusModerationQwenGuard refus = new RefusModerationQwenGuard();
        refus.setConteDanseId(conteDanseId);
        refus.setAssistant(assistant);
        refus.setStatut(analyse.status());
        refus.setPromptInitial(promptInitial);
        refus.setRetourQwenGuard(analyse.details());
        return repository.save(refus);
    }
}
