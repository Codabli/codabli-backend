package com.codabli.service;

import com.codabli.config.LlmProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LlmSystemPromptService {

    private final String prompt;

    public LlmSystemPromptService(LlmProperties properties) {
        this.prompt = charger(properties.systemPrompt());
    }

    public String prompt() {
        return prompt;
    }

    private static String charger(Resource resource) {
        try (var input = resource.getInputStream()) {
            String prompt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (prompt.isBlank()) {
                throw new IllegalStateException("La ressource du prompt systeme est vide");
            }
            return prompt;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger la ressource du prompt systeme", e);
        }
    }
}
