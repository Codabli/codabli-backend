package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.service.QwenGuardClient.SafetyAnalysis;
import com.codabli.service.QwenGuardClient.SafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Service
public class AssistantCreationUniversService {

    private static final Logger log = LoggerFactory.getLogger(AssistantCreationUniversService.class);
    private static final String PROMPTS_PATH = "classpath:prompts/assistant-creation-univers/";
    private static final String PROMPT_COMMUN = PROMPTS_PATH + "system-prompt-commun.txt";
    private static final String MESSAGE_UNSAFE =
            "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.";
    private static final String MESSAGE_CONTROVERSIAL =
            "Merci de reformuler votre demande en évitant les sujets sensibles.";
    private static final String MESSAGE_QWEN_GUARD_INDISPONIBLE =
            "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.";

    private final LlmClient llmClient;
    private final QwenGuardClient qwenGuardClient;
    private final PromptAnonymizerService promptAnonymizerService;
    private final ResourceLoader resourceLoader;

    public AssistantCreationUniversService(LlmClient llmClient,
                                           QwenGuardClient qwenGuardClient,
                                           PromptAnonymizerService promptAnonymizerService,
                                           ResourceLoader resourceLoader) {
        this.llmClient = llmClient;
        this.qwenGuardClient = qwenGuardClient;
        this.promptAnonymizerService = promptAnonymizerService;
        this.resourceLoader = resourceLoader;
    }

    public LlmResponse streamerCreationUnivers(AssistantCreationUniversRequest request, Consumer<String> onToken) {
        String descriptionAnonymisee = promptAnonymizerService.anonymize(request.description());
        SafetyAnalysis safetyAnalysis;
        try {
            safetyAnalysis = qwenGuardClient.analyser(descriptionAnonymisee);
        } catch (QwenGuardIndisponibleException e) {
            log.warn("QwenGuard indisponible pour assistant-creation-univers: {}", e.getMessage());
            return envoyerControleIndisponible(onToken);
        }
        if (safetyAnalysis == null || safetyAnalysis.status() == null) {
            log.warn("QwenGuard sans statut exploitable pour assistant-creation-univers");
            return envoyerControleIndisponible(onToken);
        }

        String details = safetyAnalysis.details() == null
                ? "" : safetyAnalysis.details().replaceAll("[\\r\\n]+", " | ");
        log.info("QwenGuard pour assistant-creation-univers: statut={}, details={}",
                safetyAnalysis.status(), details);
        return switch (safetyAnalysis.status()) {
            case SAFE -> {
                String systemPrompt = composerPrompt(request.type());
                yield llmClient.stream(LlmRequest.of(descriptionAnonymisee), systemPrompt, onToken);
            }
            case UNSAFE -> envoyerReponse(MESSAGE_UNSAFE, onToken);
            case CONTROVERSIAL -> envoyerReponse(MESSAGE_CONTROVERSIAL, onToken);
        };
    }

    String composerPrompt(AssistantCreationUniversRequest.Type type) {
        String fichierSpecialise = switch (type) {
            case Lieu -> "system-prompt-lieu.txt";
            case Personnage -> "system-prompt-personnage.txt";
            case Epoque -> "system-prompt-epoque.txt";
            case Objet -> "system-prompt-objet.txt";
        };
        return chargerPrompt(PROMPT_COMMUN) + "\n\n" + chargerPrompt(PROMPTS_PATH + fichierSpecialise);
    }

    private LlmResponse envoyerControleIndisponible(Consumer<String> onToken) {
        onToken.accept(MESSAGE_QWEN_GUARD_INDISPONIBLE);
        return LlmResponse.standby(MESSAGE_QWEN_GUARD_INDISPONIBLE);
    }

    private LlmResponse envoyerReponse(String texte, Consumer<String> onToken) {
        onToken.accept(texte);
        return LlmResponse.ok(texte, "", "stop", null, null);
    }

    private String chargerPrompt(String chemin) {
        Resource resource = resourceLoader.getResource(chemin);
        try (var input = resource.getInputStream()) {
            String prompt = new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
            if (prompt.isBlank()) {
                throw new IllegalStateException("Le fichier de prompt est vide: " + chemin);
            }
            return prompt;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger le fichier de prompt: " + chemin, e);
        }
    }
}
