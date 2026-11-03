package com.codabli.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "codabli.qwen-guard")
public record QwenGuardProperties(
        @NotBlank String model,
        @NotNull Duration timeout,
        @NotNull @Positive Integer maxTokens) {

    public QwenGuardProperties {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("codabli.qwen-guard.timeout doit etre positif");
        }
    }
}
