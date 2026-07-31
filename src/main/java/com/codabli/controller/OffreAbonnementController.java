package com.codabli.controller;

import com.codabli.dto.OffreAbonnementRequest;
import com.codabli.dto.OffreAbonnementResponse;
import com.codabli.entity.enums.PublicAbonnement;
import com.codabli.service.AbonnementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour le catalogue des offres d'abonnement (CDC 8.14, ABO-01).
 *
 * Securite :
 * - GET (catalogue) : public
 * - POST / PUT / DELETE (sous /api/admin/**) : reserves aux admins
 */
@RestController
public class OffreAbonnementController {

    private final AbonnementService abonnementService;

    public OffreAbonnementController(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    /**
     * GET /api/abonnements/offres
     * Liste les offres actives. Filtre optionnel : ?publicCible=famille
     */
    @GetMapping("/api/abonnements/offres")
    public ResponseEntity<List<OffreAbonnementResponse>> lister(
            @RequestParam(required = false) PublicAbonnement publicCible) {
        return ResponseEntity.ok(abonnementService.listerOffres(publicCible));
    }

    /**
     * GET /api/abonnements/offres/{id}
     */
    @GetMapping("/api/abonnements/offres/{id}")
    public ResponseEntity<OffreAbonnementResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(abonnementService.getOffreById(id));
    }

    /**
     * POST /api/admin/abonnements/offres
     * Cree une offre d'abonnement. Reserve aux admins.
     */
    @PostMapping("/api/admin/abonnements/offres")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<OffreAbonnementResponse> creer(@Valid @RequestBody OffreAbonnementRequest request) {
        OffreAbonnementResponse response = abonnementService.creerOffre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/admin/abonnements/offres/{id}
     * Modifie une offre d'abonnement. Reserve aux admins.
     */
    @PutMapping("/api/admin/abonnements/offres/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<OffreAbonnementResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody OffreAbonnementRequest request) {
        return ResponseEntity.ok(abonnementService.modifierOffre(id, request));
    }

    /**
     * DELETE /api/admin/abonnements/offres/{id}
     * Supprime une offre d'abonnement. Reserve aux admins.
     */
    @DeleteMapping("/api/admin/abonnements/offres/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        abonnementService.supprimerOffre(id);
        return ResponseEntity.noContent().build();
    }
}
