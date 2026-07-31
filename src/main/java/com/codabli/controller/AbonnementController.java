package com.codabli.controller;

import com.codabli.dto.AbonnementRequest;
import com.codabli.dto.AbonnementResponse;
import com.codabli.service.AbonnementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour les souscriptions d'abonnement (CDC 8.14, ABO-02/03/04).
 *
 * Securite : tous les endpoints necessitent une authentification. La
 * verification fine (propriete de l'abonnement) est geree dans le service.
 */
@RestController
@RequestMapping("/api/abonnements")
public class AbonnementController {

    private final AbonnementService abonnementService;

    public AbonnementController(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    /**
     * POST /api/abonnements
     * Souscrit a une offre (ABO-02).
     */
    @PostMapping
    public ResponseEntity<AbonnementResponse> souscrire(
            @Valid @RequestBody AbonnementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AbonnementResponse response = abonnementService.souscrire(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/abonnements/mes-abonnements
     * Liste les abonnements de l'utilisateur connecte, avec statut, dates
     * d'expiration et facture (ABO-03/04).
     */
    @GetMapping("/mes-abonnements")
    public ResponseEntity<List<AbonnementResponse>> mesAbonnements(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(abonnementService.mesAbonnements(jwt));
    }

    /**
     * PATCH /api/abonnements/{id}/renouveler (ABO-03)
     */
    @PatchMapping("/{id}/renouveler")
    public ResponseEntity<AbonnementResponse> renouveler(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(abonnementService.renouveler(id, jwt));
    }

    /**
     * PATCH /api/abonnements/{id}/resilier (ABO-03)
     */
    @PatchMapping("/{id}/resilier")
    public ResponseEntity<AbonnementResponse> resilier(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(abonnementService.resilier(id, jwt));
    }

    /**
     * PATCH /api/abonnements/{id}/changer (ABO-03)
     * Change l'abonnement vers une nouvelle offre. Body : { "offreId": "..." }
     */
    @PatchMapping("/{id}/changer")
    public ResponseEntity<AbonnementResponse> changerOffre(
            @PathVariable UUID id,
            @Valid @RequestBody AbonnementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(abonnementService.changerOffre(id, request, jwt));
    }
}
