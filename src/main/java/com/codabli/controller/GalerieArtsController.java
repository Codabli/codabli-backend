package com.codabli.controller;

import com.codabli.dto.FresqueRequest;
import com.codabli.dto.FresqueResponse;
import com.codabli.dto.HotspotRequest;
import com.codabli.dto.SalleGalerieRequest;
import com.codabli.dto.SalleGalerieResponse;
import com.codabli.service.GalerieArtsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour le socle de la Galerie des Arts immersive (CDC 8.11).
 *
 * Securite : GET public (hall + salles + fresques + hotspots actifs) ;
 * gestion du contenu reservee a admin/super_admin.
 */
@RestController
@RequestMapping("/api/galerie-arts")
public class GalerieArtsController {

    private final GalerieArtsService galerieArtsService;

    public GalerieArtsController(GalerieArtsService galerieArtsService) {
        this.galerieArtsService = galerieArtsService;
    }

    /** GET /api/galerie-arts/salles — le hall (GAL-01). */
    @GetMapping("/salles")
    public ResponseEntity<List<SalleGalerieResponse>> listerSalles() {
        return ResponseEntity.ok(galerieArtsService.listerSalles());
    }

    /** GET /api/galerie-arts/salles/{id} — salle + fresque + hotspots (GAL-03/06/08). */
    @GetMapping("/salles/{id}")
    public ResponseEntity<SalleGalerieResponse> getSalle(@PathVariable UUID id) {
        return ResponseEntity.ok(galerieArtsService.getSalle(id));
    }

    @PostMapping("/salles")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<SalleGalerieResponse> creerSalle(@Valid @RequestBody SalleGalerieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galerieArtsService.creerSalle(request));
    }

    @PutMapping("/salles/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<SalleGalerieResponse> modifierSalle(@PathVariable UUID id,
            @Valid @RequestBody SalleGalerieRequest request) {
        return ResponseEntity.ok(galerieArtsService.modifierSalle(id, request));
    }

    @DeleteMapping("/salles/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimerSalle(@PathVariable UUID id) {
        galerieArtsService.supprimerSalle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fresques")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<FresqueResponse> creerFresque(@Valid @RequestBody FresqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galerieArtsService.creerFresque(request));
    }

    @PutMapping("/fresques/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<FresqueResponse> modifierFresque(@PathVariable UUID id,
            @Valid @RequestBody FresqueRequest request) {
        return ResponseEntity.ok(galerieArtsService.modifierFresque(id, request));
    }

    @DeleteMapping("/fresques/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimerFresque(@PathVariable UUID id) {
        galerieArtsService.supprimerFresque(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fresques/{fresqueId}/hotspots")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<FresqueResponse> ajouterHotspot(@PathVariable UUID fresqueId,
            @Valid @RequestBody HotspotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(galerieArtsService.ajouterHotspot(fresqueId, request));
    }

    @PutMapping("/fresques/{fresqueId}/hotspots/{hotspotId}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<FresqueResponse> modifierHotspot(@PathVariable UUID fresqueId,
            @PathVariable UUID hotspotId, @Valid @RequestBody HotspotRequest request) {
        return ResponseEntity.ok(galerieArtsService.modifierHotspot(fresqueId, hotspotId, request));
    }

    @DeleteMapping("/fresques/{fresqueId}/hotspots/{hotspotId}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<FresqueResponse> supprimerHotspot(@PathVariable UUID fresqueId,
            @PathVariable UUID hotspotId) {
        return ResponseEntity.ok(galerieArtsService.supprimerHotspot(fresqueId, hotspotId));
    }
}
