package com.codabli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret
) {
}
