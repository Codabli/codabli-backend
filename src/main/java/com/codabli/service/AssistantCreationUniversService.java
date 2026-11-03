package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AssistantCreationUniversService {

    private static final Logger log = LoggerFactory.getLogger(AssistantCreationUniversService.class);
    private static final String PROMPTS_PATH = "classpath:prompts/assistant-creation-univers/";
    private static final String PROMPT_COMMUN = PROMPTS_PATH + "system-prompt-commun.txt";

    private final LlmClient llmClient;
    private final AssistantPromptModerationService promptModerationService;
    private final PromptAnonymizerService promptAnonymizerService;
    private final ResourceLoader resourceLoader;

    public AssistantCreationUniversService(LlmClient llmClient,
                                           AssistantPromptModerationService promptModerationService,
                                           PromptAnonymizerService promptAnonymizerService,
                                           ResourceLoader resourceLoader) {
        this.llmClient = llmClient;
        this.promptModerationService = promptModerationService;
        this.promptAnonymizerService = promptAnonymizerService;
        this.resourceLoader = resourceLoader;
    }

    public LlmResponse streamerCreationUnivers(UUID conteDanseId,
                                               AssistantCreationUniversRequest request,
                                               Consumer<String> onToken) {
        String descriptionAnonymisee = promptAnonymizerService.anonymize(request.description());
        Optional<LlmResponse> moderationResponse =
                promptModerationService.moderer("assistant-creation-univers", conteDanseId,
                        descriptionAnonymisee, onToken);
        if (moderationResponse.isPresent()) {
            return moderationResponse.get();
        }
        String systemPrompt = composerPrompt(request.type());
        return llmClient.stream(LlmRequest.of(descriptionAnonymisee), systemPrompt, onToken);
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
