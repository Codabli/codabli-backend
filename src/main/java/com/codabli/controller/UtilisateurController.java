package com.codabli.controller;

import com.codabli.dto.UtilisateurResponse;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints for managing users.
 * All endpoints under /api/admin/** require the "admin" role.
 */
@RestController
@RequestMapping("/api/admin/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /**
     * GET /api/admin/utilisateurs — Lists all users.
     * Optionally filter by role using query parameter ?role=enseignant
     */
    @GetMapping
    public ResponseEntity<List<UtilisateurResponse>> getAllUsers(
            @RequestParam(required = false) RoleUtilisateur role) {
        List<UtilisateurResponse> users;
        if (role != null) {
            users = utilisateurService.getUsersByRole(role);
        } else {
            users = utilisateurService.getAllUsers();
        }
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/utilisateurs/{id} — Returns a specific user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponse> getUserById(@PathVariable UUID id) {
        UtilisateurResponse response = utilisateurService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/admin/utilisateurs/{id} — Deletes a user from Keycloak and
     * database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        utilisateurService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/admin/utilisateurs/{id}/statut — Changes user status (actif,
     * inactif, suspendu).
     * Request body: { "statut": "inactif" }
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<UtilisateurResponse> changeStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("statut");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Le champ 'statut' est obligatoire");
        }
        UtilisateurResponse response = utilisateurService.changeUserStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }
}
