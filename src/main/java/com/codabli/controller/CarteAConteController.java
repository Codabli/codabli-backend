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
import java.util.Map;
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
    @PreAuthorize("hasAnyRole('eleve', 'enseignant', 'admin', 'super_admin', 'moderateur')")
    public ResponseEntity<List<CarteAConteResponse>> lister(@AuthenticationPrincipal Jwt jwt) {
        List<CarteAConteResponse> cartes = carteAConteService.listerSelonRole(jwt);
        return ResponseEntity.ok(cartes);
    }

    /**
     * PATCH /api/cartes-a-conte/{id}/valider
     * Valide une carte (MOD-02). Enseignant (limite a ses classes) ou
     * admin/super_admin/moderateur (droit total).
     */
    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('enseignant', 'admin', 'super_admin', 'moderateur')")
    public ResponseEntity<CarteAConteResponse> valider(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        CarteAConteResponse response = carteAConteService.valider(id, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/cartes-a-conte/{id}/refuser
     * Refuse une carte (MOD-03). Body optionnel : { "motif": "..." }
     */
    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('enseignant', 'admin', 'super_admin', 'moderateur')")
    public ResponseEntity<CarteAConteResponse> refuser(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String motif = body != null ? body.get("motif") : null;
        CarteAConteResponse response = carteAConteService.refuser(id, motif, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/cartes-a-conte/{id}/demander-correction
     * Demande une correction a l'auteur (MOD-04). Body optionnel : { "motif":
     * "..." }
     */
    @PatchMapping("/{id}/demander-correction")
    @PreAuthorize("hasAnyRole('enseignant', 'admin', 'super_admin', 'moderateur')")
    public ResponseEntity<CarteAConteResponse> demanderCorrection(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String motif = body != null ? body.get("motif") : null;
        CarteAConteResponse response = carteAConteService.demanderCorrection(id, motif, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/cartes-a-conte/{id}/retirer
     * Retire une carte precedemment validee/publiee (MOD-05). Reserve aux
     * admin/super_admin/moderateur.
     */
    @PatchMapping("/{id}/retirer")
    @PreAuthorize("hasAnyRole('admin', 'super_admin', 'moderateur')")
    public ResponseEntity<CarteAConteResponse> retirer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        CarteAConteResponse response = carteAConteService.retirer(id, jwt);
        return ResponseEntity.ok(response);
    }
}
