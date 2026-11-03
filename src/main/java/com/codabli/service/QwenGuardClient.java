package com.codabli.service;

/**
 * Controle de securite des prompts avant leur transmission au LLM.
 */
public interface QwenGuardClient {

    SafetyAnalysis analyser(String prompt);

    enum SafetyStatus {
        SAFE,
        UNSAFE,
        CONTROVERSIAL
    }

    record SafetyAnalysis(SafetyStatus status, String details) {
    }
}
