package com.codabli.controller;

import com.codabli.dto.PartenaireRequest;
import com.codabli.dto.PartenaireResponse;
import com.codabli.entity.enums.CategoriePartenaire;
import com.codabli.service.PartenaireService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pour les partenaires (CDC 8.13, PAR-01/02).
 *
 * Securite :
 * - GET (liste + detail) : public, filtre sur actif=true
 * - POST / PUT / DELETE : reserves aux admins via @PreAuthorize
 */
@RestController
@RequestMapping("/api/partenaires")
public class PartenaireController {

    private final PartenaireService partenaireService;

    public PartenaireController(PartenaireService partenaireService) {
        this.partenaireService = partenaireService;
    }

    /**
     * GET /api/partenaires
     * Liste paginee des partenaires actifs. Filtre optionnel : ?categorie=culturel
     */
    @GetMapping
    public ResponseEntity<Page<PartenaireResponse>> lister(
            @RequestParam(required = false) CategoriePartenaire categorie,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PartenaireResponse> partenaires = partenaireService.lister(categorie, pageable);
        return ResponseEntity.ok(partenaires);
    }

    /**
     * GET /api/partenaires/{id}
     * Detail d'un partenaire.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PartenaireResponse> getById(@PathVariable UUID id) {
        PartenaireResponse response = partenaireService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/partenaires
     * Cree un partenaire. Reserve aux admins.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<PartenaireResponse> creer(@Valid @RequestBody PartenaireRequest request) {
        PartenaireResponse response = partenaireService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/partenaires/{id}
     * Modifie un partenaire. Reserve aux admins.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<PartenaireResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody PartenaireRequest request) {
        PartenaireResponse response = partenaireService.modifier(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/partenaires/{id}
     * Supprime un partenaire. Reserve aux admins.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        partenaireService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
