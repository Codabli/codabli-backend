package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Service
public class AssistantCreationUniversService {

    private static final String PROMPTS_PATH = "classpath:prompts/assistant-creation-univers/";
    private static final String PROMPT_COMMUN = PROMPTS_PATH + "system-prompt-commun.txt";

    private final LlmClient llmClient;
    private final ResourceLoader resourceLoader;

    public AssistantCreationUniversService(LlmClient llmClient, ResourceLoader resourceLoader) {
        this.llmClient = llmClient;
        this.resourceLoader = resourceLoader;
    }

    public LlmResponse streamerCreationUnivers(AssistantCreationUniversRequest request, Consumer<String> onToken) {
        String systemPrompt = composerPrompt(request.type());
        return llmClient.stream(LlmRequest.of(request.description()), systemPrompt, onToken);
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
