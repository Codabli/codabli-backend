package com.codabli.controller;

import com.codabli.dto.*;
import com.codabli.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints and authenticated user profile management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /**
     * POST /api/auth/register — Public endpoint to create a new account.
     * Creates the user in Keycloak and in the local database.
     */
    @PostMapping("/register")
    public ResponseEntity<UtilisateurResponse> register(@Valid @RequestBody RegisterRequest request) {
        UtilisateurResponse response = utilisateurService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login — Public endpoint to authenticate and obtain JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = utilisateurService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me — Returns the profile of the currently authenticated user.
     * The Keycloak user ID is extracted from the JWT "sub" claim.
     */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        UtilisateurResponse response = utilisateurService.getCurrentUser(keycloakId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/auth/me — Updates the profile of the currently authenticated user.
     */
    @PutMapping("/me")
    public ResponseEntity<UtilisateurResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUtilisateurRequest request) {
        String keycloakId = jwt.getSubject();
        UtilisateurResponse response = utilisateurService.updateProfile(keycloakId, request);
        return ResponseEntity.ok(response);
    }
}
