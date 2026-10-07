package com.codabli.service;

import com.codabli.config.KeycloakProperties;
import com.codabli.dto.LoginResponse;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for managing users in Keycloak via the Admin REST API.
 * Handles user creation, authentication (token retrieval), password updates,
 * and deletion.
 */
@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakAdminService(Keycloak keycloak, RestClient restClient, KeycloakProperties properties) {
        this.keycloak = keycloak;
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Creates a new user in Keycloak with the given credentials and assigns a realm
     * role.
     *
     * @return the Keycloak user ID
     * @throws RuntimeException if user creation fails
     */
    public String createUser(String email, String password, String firstName, String lastName, String roleName) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(Collections.singletonList(credential));

        UsersResource usersResource = getRealmResource().users();

        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == 201) {
                String userId = extractUserIdFromLocation(response);
                assignRealmRole(userId, roleName);
                return userId;
            } else if (response.getStatus() == 409) {
                throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà dans Keycloak");
            } else {
                throw new RuntimeException(
                        "Erreur lors de la création de l'utilisateur Keycloak: " + response.getStatusInfo());
            }
        }
    }

    /**
     * Authenticates a user and returns JWT tokens from Keycloak's token endpoint.
     */
    public LoginResponse login(String email, String password) {
        try {
            Map<String, Object> responseBody = restClient.post()
                    .uri("/realms/" + properties.realm() + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(Map.of(
                            "grant_type", "password",
                            "client_id", properties.clientId(),
                            "client_secret", properties.clientSecret(),
                            "username", email,
                            "password", password
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (responseBody == null) {
                throw new RuntimeException("Réponse vide du serveur Keycloak");
            }

            return new LoginResponse(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token"),
                    ((Number) responseBody.get("expires_in")).longValue(),
                    (String) responseBody.get("token_type"));
        } catch (org.springframework.web.client.HttpClientErrorException _) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }
    }

    /**
     * Deletes a user from Keycloak by their Keycloak ID.
     */
    public void deleteUser(String keycloakId) {
        getRealmResource().users().get(keycloakId).remove();
    }

    /**
     * Updates the password for a Keycloak user.
     */
    public void updatePassword(String keycloakId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        getRealmResource().users().get(keycloakId).resetPassword(credential);
    }

    private RealmResource getRealmResource() {
        return keycloak.realm(properties.realm());
    }

    private void assignRealmRole(String userId, String roleName) {
        RoleRepresentation role = getRealmResource().roles().get(roleName).toRepresentation();
        getRealmResource().users().get(userId)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(role));
    }

    private String extractUserIdFromLocation(Response response) {
        String locationHeader = response.getHeaderString("Location");
        if (locationHeader != null) {
            String[] parts = locationHeader.split("/");
            return parts[parts.length - 1];
        }
        // Fallback: search by username
        List<UserRepresentation> users = getRealmResource().users()
                .search(null, null, null, null, 0, 1);
        if (!users.isEmpty()) {
            return users.getFirst().getId();
        }
        throw new RuntimeException("Impossible de récupérer l'ID de l'utilisateur créé");
    }
}
