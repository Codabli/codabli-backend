package com.codabli.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmClientConfigTest {

    @Test
    void shouldAcceptValidOvhcloudHttpsEndpoint() {
        assertThatCode(() -> new LlmClientConfig(
                properties("https://oai.endpoints.kepler.ai.cloud.ovh.net/v1"))).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEndpointWhoseHostSpoofsOvhcloudDomain() {
        assertThatThrownBy(() -> new LlmClientConfig(
                properties("https://oai.endpoints.kepler.ai.cloud.ovh.net.attacker.test/v1"))).isInstanceOf(
                IllegalStateException.class).hasMessageContaining("OVHcloud AI");
    }

    private static LlmProperties properties(String baseUrl) {
        return new LlmProperties(baseUrl, "test-key", "test-model",
                new ByteArrayResource("system-prompt".getBytes(StandardCharsets.UTF_8)), 0.3, 256,
                Duration.ofSeconds(5), "standby");
    }
}
