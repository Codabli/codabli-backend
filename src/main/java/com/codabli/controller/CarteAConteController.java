package com.codabli.controller;

import com.codabli.dto.CarteAConteRequest;
import com.codabli.dto.CarteAConteResponse;
import com.codabli.service.CarteAConteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour les cartes a conte.
 *
 * Autorisation a deux niveaux :
 * 1. @PreAuthorize → verifie le role (grossier)
 * 2. Le service → verifie la relation (fin)
 *
 * Exemples :
 * - POST : seul un eleve peut creer → @PreAuthorize("hasRole('eleve')")
 * - GET : eleve/enseignant/admin avec filtrage dans le service
 * - PATCH valider : seul un enseignant, et seulement si l'eleve est dans sa
 * classe
 */
@RestController
@RequestMapping("/api/cartes-a-conte")
public class CarteAConteController {

    private final CarteAConteService carteAConteService;

    public CarteAConteController(CarteAConteService carteAConteService) {
        this.carteAConteService = carteAConteService;
    }

    /**
     * POST /api/cartes-a-conte
     * Cree une carte a conte. Seul un eleve peut creer.
     * Le createur est identifie via le JWT, pas via le body.
     */
    @PostMapping
    @PreAuthorize("hasRole('eleve')")
    public ResponseEntity<CarteAConteResponse> creer(
            @Valid @RequestBody CarteAConteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CarteAConteResponse response = carteAConteService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/cartes-a-conte
     * Liste les cartes visibles selon le role :
     * - eleve → ses propres cartes
     * - enseignant → cartes des eleves de sa classe
     * - admin → toutes les cartes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('eleve', 'enseignant', 'admin')")
    public ResponseEntity<List<CarteAConteResponse>> lister(@AuthenticationPrincipal Jwt jwt) {
        List<CarteAConteResponse> cartes = carteAConteService.listerSelonRole(jwt);
        return ResponseEntity.ok(cartes);
    }

    /**
     * PATCH /api/cartes-a-conte/{id}/valider
     * Valide une carte. Seul un enseignant peut valider,
     * et seulement si l'eleve createur est dans sa classe.
     */
    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('enseignant')")
    public ResponseEntity<CarteAConteResponse> valider(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        CarteAConteResponse response = carteAConteService.valider(id, jwt);
        return ResponseEntity.ok(response);
    }
}
