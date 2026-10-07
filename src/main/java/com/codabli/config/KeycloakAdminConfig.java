package com.codabli.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the Keycloak Admin Client bean used for user management
 * operations (create, delete, update password, etc.) via the Keycloak Admin
 * REST API.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdminClient(KeycloakProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm(properties.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .build();
    }

    @Bean
    public RestClient restClient(KeycloakProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverUrl())
                .build();
    }
}
