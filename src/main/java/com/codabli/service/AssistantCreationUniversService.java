package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.ConteContextData;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final ConteContextService conteContextService;
    private final ConteContextFormatter conteContextFormatter;

    public AssistantCreationUniversService(LlmClient llmClient,
                                           AssistantPromptModerationService promptModerationService,
                                           PromptAnonymizerService promptAnonymizerService,
                                           ResourceLoader resourceLoader,
                                           ConteContextService conteContextService,
                                           ConteContextFormatter conteContextFormatter) {
        this.llmClient = llmClient;
        this.promptModerationService = promptModerationService;
        this.promptAnonymizerService = promptAnonymizerService;
        this.resourceLoader = resourceLoader;
        this.conteContextService = conteContextService;
        this.conteContextFormatter = conteContextFormatter;
    }

    public ConversationPreparee preparerCreationUnivers(UUID conteDanseId,
                                                        AssistantCreationUniversRequest request,
                                                        Jwt jwt) {
        ConteContextData contexteConte = conteContextService.charger(conteDanseId, jwt);
        return new ConversationPreparee(conteDanseId, request, contexteConte);
    }

    public LlmResponse streamerCreationUnivers(ConversationPreparee conversation, Consumer<String> onToken) {
        UUID conteDanseId = conversation.conteDanseId();
        AssistantCreationUniversRequest request = conversation.request();
        String descriptionAnonymisee = promptAnonymizerService.anonymize(request.description());
        Optional<LlmResponse> moderationResponse =
                promptModerationService.moderer("assistant-creation-univers", conteDanseId,
                        descriptionAnonymisee, onToken);
        if (moderationResponse.isPresent()) {
            return moderationResponse.get();
        }
        String systemPrompt = conteContextFormatter.ajouterAuPrompt(
                composerPrompt(request.type()), conversation.contexteConte());
        return llmClient.stream(LlmRequest.of(descriptionAnonymisee), systemPrompt, onToken);
    }

    public record ConversationPreparee(
            UUID conteDanseId,
            AssistantCreationUniversRequest request,
            ConteContextData contexteConte) {
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
