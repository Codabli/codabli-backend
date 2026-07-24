package com.codabli.controller;

import com.codabli.dto.GalerieMiseEnAvantRequest;
import com.codabli.dto.GalerieMiseEnAvantResponse;
import com.codabli.service.GalerieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller admin pour la gestion des mises en avant dans la galerie.
 *
 * Sécurité : protégé par la règle SecurityConfig existante
 * requestMatchers("/api/admin/**").hasRole("admin").
 * Aucun @PreAuthorize additionnel nécessaire.
 */
@RestController
@RequestMapping("/api/admin/galerie/mise-en-avant")
@Tag(name = "Galerie Admin", description = "Gestion des mises en avant (admin uniquement)")
public class GalerieAdminController {

    private final GalerieService galerieService;

    public GalerieAdminController(GalerieService galerieService) {
        this.galerieService = galerieService;
    }

    /**
     * GET /api/admin/galerie/mise-en-avant
     * Liste toutes les mises en avant.
     */
    @GetMapping
    @Operation(summary = "Lister les mises en avant", description = "Retourne toutes les mises en avant (actives et inactives)")
    @ApiResponse(responseCode = "200", description = "Liste des mises en avant")
    public ResponseEntity<List<GalerieMiseEnAvantResponse>> lister() {
        List<GalerieMiseEnAvantResponse> mises = galerieService.listerMisesEnAvant();
        return ResponseEntity.ok(mises);
    }

    /**
     * POST /api/admin/galerie/mise-en-avant
     * Ajoute une mise en avant pour une carte validée.
     */
    @PostMapping
    @Operation(summary = "Ajouter une mise en avant", description = "Crée une mise en avant. La carte doit exister et être validée.")
    @ApiResponse(responseCode = "201", description = "Mise en avant créée")
    @ApiResponse(responseCode = "404", description = "Carte non trouvée ou non validée")
    public ResponseEntity<GalerieMiseEnAvantResponse> ajouter(
            @Valid @RequestBody GalerieMiseEnAvantRequest request) {
        GalerieMiseEnAvantResponse response = galerieService.ajouterMiseEnAvant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/admin/galerie/mise-en-avant/{id}
     * Supprime une mise en avant.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une mise en avant")
    @ApiResponse(responseCode = "204", description = "Mise en avant supprimée")
    @ApiResponse(responseCode = "404", description = "Mise en avant non trouvée")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        galerieService.supprimerMiseEnAvant(id);
        return ResponseEntity.noContent().build();
    }
}
