package com.codabli.controller;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.service.GalerieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller public pour la galerie d'art.
 *
 * Sécurité : aucun @PreAuthorize — les endpoints sont publics.
 * Le SecurityConfig autorise GET /api/galerie/** sans authentification.
 */
@RestController
@RequestMapping("/api/galerie")
@Tag(name = "Galerie d'Art", description = "Catalogue public des cartes à conte validées")
public class GalerieController {

    private final GalerieService galerieService;

    public GalerieController(GalerieService galerieService) {
        this.galerieService = galerieService;
    }

    /**
     * GET /api/galerie
     * Liste paginée des cartes validées. Les mises en avant actives
     * apparaissent en premier.
     */
    @GetMapping
    @Operation(summary = "Lister la galerie", description = "Retourne les cartes validées, mises en avant en premier")
    @ApiResponse(responseCode = "200", description = "Liste paginée des cartes")
    public ResponseEntity<Page<GalerieItemResponse>> lister(
            @PageableDefault(size = 12) Pageable pageable) {
        Page<GalerieItemResponse> galerie = galerieService.listerGalerie(pageable);
        return ResponseEntity.ok(galerie);
    }

    /**
     * GET /api/galerie/{id}
     * Détail d'une carte validée. Retourne 404 si la carte n'existe pas
     * ou n'est pas validée (pas 403, pour ne pas exposer l'existence
     * de cartes non validées).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une carte", description = "Retourne le détail d'une carte validée. 404 si non trouvée ou non validée.")
    @ApiResponse(responseCode = "200", description = "Détail de la carte")
    @ApiResponse(responseCode = "404", description = "Carte non trouvée ou non validée")
    public ResponseEntity<GalerieItemResponse> getById(@PathVariable UUID id) {
        GalerieItemResponse response = galerieService.getCarteGalerie(id);
        return ResponseEntity.ok(response);
    }
}
