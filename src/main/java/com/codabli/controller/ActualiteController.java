package com.codabli.controller;

import com.codabli.dto.ActualiteRequest;
import com.codabli.dto.ActualiteResponse;
import com.codabli.service.ActualiteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pour les actualites.
 *
 * Securite :
 * - GET (liste + detail) : public, aucune authentification requise
 * - POST / PUT / DELETE : reserve aux admins via @PreAuthorize
 */
@RestController
@RequestMapping("/api/actualites")
public class ActualiteController {

    private final ActualiteService actualiteService;

    public ActualiteController(ActualiteService actualiteService) {
        this.actualiteService = actualiteService;
    }

    /**
     * GET /api/actualites
     * Liste paginee des actualites publiees, triee par date de publication.
     * Parametres de pagination : ?page=0&size=10&sort=datePublication,desc
     */
    @GetMapping
    public ResponseEntity<Page<ActualiteResponse>> lister(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ActualiteResponse> actualites = actualiteService.listerPubliees(pageable);
        return ResponseEntity.ok(actualites);
    }

    /**
     * GET /api/actualites/{id}
     * Detail d'une actualite.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActualiteResponse> getById(@PathVariable UUID id) {
        ActualiteResponse response = actualiteService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/actualites
     * Cree une actualite. Reserve aux admins.
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ActualiteResponse> creer(
            @Valid @RequestBody ActualiteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ActualiteResponse response = actualiteService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/actualites/{id}
     * Modifie une actualite. Reserve aux admins.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ActualiteResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ActualiteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ActualiteResponse response = actualiteService.modifier(id, request, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/actualites/{id}
     * Supprime une actualite. Reserve aux admins.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        actualiteService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
