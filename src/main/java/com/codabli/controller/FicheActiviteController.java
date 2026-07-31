package com.codabli.controller;

import com.codabli.dto.FicheActiviteRequest;
import com.codabli.dto.FicheActiviteResponse;
import com.codabli.service.FicheActiviteService;
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
 * Controller pour les fiches d'activites (CDC 8.7, FIC-ACT-01/02).
 *
 * Securite, identique a la Mallette Pedagogique :
 * - GET : enseignant/professionnel_education/admin/super_admin/comite_lecture
 * - POST/PUT : admin/super_admin/comite_lecture
 * - DELETE : admin/super_admin uniquement
 */
@RestController
@RequestMapping("/api/fiches-activites")
public class FicheActiviteController {

    private final FicheActiviteService ficheActiviteService;

    public FicheActiviteController(FicheActiviteService ficheActiviteService) {
        this.ficheActiviteService = ficheActiviteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin', 'comite_lecture')")
    public ResponseEntity<Page<FicheActiviteResponse>> lister(
            @RequestParam(required = false) Integer age,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ficheActiviteService.lister(age, jwt, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin', 'comite_lecture')")
    public ResponseEntity<FicheActiviteResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ficheActiviteService.getById(id, jwt));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'super_admin', 'comite_lecture')")
    public ResponseEntity<FicheActiviteResponse> creer(@Valid @RequestBody FicheActiviteRequest request) {
        FicheActiviteResponse response = ficheActiviteService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin', 'comite_lecture')")
    public ResponseEntity<FicheActiviteResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody FicheActiviteRequest request) {
        return ResponseEntity.ok(ficheActiviteService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        ficheActiviteService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
