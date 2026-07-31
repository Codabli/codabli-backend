package com.codabli.controller;

import com.codabli.dto.ProfilEnfantRequest;
import com.codabli.dto.ProfilEnfantResponse;
import com.codabli.service.ProfilEnfantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour les profils enfants (CDC 8.16/8.17, FAM-02, ENF-01).
 *
 * Securite :
 * - Toutes les operations necessitent une authentification.
 * - Creation reservee aux comptes parent/professionnel_education.
 * - Lecture/modification/suppression : reservees au responsable proprietaire
 * du profil (ou admin/super_admin), verifie dans le service.
 */
@RestController
@RequestMapping("/api/profils-enfants")
public class ProfilEnfantController {

    private final ProfilEnfantService profilEnfantService;

    public ProfilEnfantController(ProfilEnfantService profilEnfantService) {
        this.profilEnfantService = profilEnfantService;
    }

    /**
     * POST /api/profils-enfants
     * Cree un profil enfant (FAM-02). Reserve aux comptes parent ou
     * professionnel de l'education.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('parent', 'professionnel_education')")
    public ResponseEntity<ProfilEnfantResponse> creer(
            @Valid @RequestBody ProfilEnfantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProfilEnfantResponse response = profilEnfantService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/profils-enfants
     * Liste les profils enfants du responsable connecte.
     */
    @GetMapping
    public ResponseEntity<List<ProfilEnfantResponse>> mesProfils(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profilEnfantService.mesProfils(jwt));
    }

    /**
     * GET /api/profils-enfants/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfilEnfantResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profilEnfantService.getById(id, jwt));
    }

    /**
     * PUT /api/profils-enfants/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfilEnfantResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ProfilEnfantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profilEnfantService.modifier(id, request, jwt));
    }

    /**
     * DELETE /api/profils-enfants/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        profilEnfantService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
