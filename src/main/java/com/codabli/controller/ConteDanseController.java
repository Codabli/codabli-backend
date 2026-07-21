package com.codabli.controller;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.service.ConteDanseService;
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
 * Controller pour les contes danses.
 *
 * Securite a deux niveaux :
 * 1. @PreAuthorize → verifie le role (grossier)
 * 2. Le service → verifie la relation auteur/admin (fin)
 *
 * Acces :
 * - GET (catalogue) : public, aucune authentification requise
 * - POST : utilisateur authentifie
 * - PUT / DELETE : utilisateur authentifie + verification auteur ou admin dans
 * le service
 */
@RestController
@RequestMapping("/api/contes-danses")
public class ConteDanseController {

    private final ConteDanseService conteDanseService;

    public ConteDanseController(ConteDanseService conteDanseService) {
        this.conteDanseService = conteDanseService;
    }

    /**
     * GET /api/contes-danses
     * Catalogue public des contes publies, pagine.
     * Parametres : ?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<ConteDanseResponse>> lister(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ConteDanseResponse> contes = conteDanseService.listerPublies(pageable);
        return ResponseEntity.ok(contes);
    }

    /**
     * GET /api/contes-danses/{id}
     * Detail d'un conte danse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConteDanseResponse> getById(@PathVariable UUID id) {
        ConteDanseResponse response = conteDanseService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/contes-danses
     * Cree un conte danse. Tout utilisateur authentifie peut creer.
     * Le createur est identifie via le JWT, pas via le body.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteDanseResponse> creer(
            @Valid @RequestBody ConteDanseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ConteDanseResponse response = conteDanseService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/contes-danses/{id}
     * Modifie un conte danse. L'auteur ou un admin peut modifier.
     * La verification fine est faite dans le service.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteDanseResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ConteDanseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ConteDanseResponse response = conteDanseService.modifier(id, request, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/contes-danses/{id}
     * Supprime un conte danse. L'auteur ou un admin peut supprimer.
     * La verification fine est faite dans le service.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        conteDanseService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
