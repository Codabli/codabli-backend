package com.codabli.controller;

import com.codabli.dto.ElementCarnetLectureRequest;
import com.codabli.dto.PageCarnetLectureRequest;
import com.codabli.dto.PageCarnetLectureResponse;
import com.codabli.service.CarnetLectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour le Carnet de Lecture (CDC 8.17.1, CDL-01 a 17).
 *
 * Securite : toutes les operations necessitent une authentification, avec
 * verification fine dans le service (seul le responsable du profil enfant,
 * ou un admin/super_admin, peut acceder au carnet — RG-13).
 */
@RestController
@RequestMapping("/api/profils-enfants/{profilEnfantId}/carnet-lecture")
public class CarnetLectureController {

    private final CarnetLectureService carnetLectureService;

    public CarnetLectureController(CarnetLectureService carnetLectureService) {
        this.carnetLectureService = carnetLectureService;
    }

    /**
     * POST .../carnet-lecture
     * Cree une nouvelle page (CDL-01).
     */
    @PostMapping
    public ResponseEntity<PageCarnetLectureResponse> creerPage(
            @PathVariable UUID profilEnfantId,
            @RequestBody PageCarnetLectureRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        PageCarnetLectureResponse response = carnetLectureService.creerPage(profilEnfantId, request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET .../carnet-lecture
     * Le carnet complet : toutes les pages de l'enfant (CDL-17).
     */
    @GetMapping
    public ResponseEntity<List<PageCarnetLectureResponse>> listerPages(
            @PathVariable UUID profilEnfantId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetLectureService.listerPages(profilEnfantId, jwt));
    }

    /**
     * GET .../carnet-lecture/{pageId}
     * Previsualisation d'une page (CDL-14).
     */
    @GetMapping("/{pageId}")
    public ResponseEntity<PageCarnetLectureResponse> getPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetLectureService.getPage(profilEnfantId, pageId, jwt));
    }

    /**
     * PUT .../carnet-lecture/{pageId}
     * Modifie le contenu d'une page.
     */
    @PutMapping("/{pageId}")
    public ResponseEntity<PageCarnetLectureResponse> modifierPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @RequestBody PageCarnetLectureRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetLectureService.modifierPage(profilEnfantId, pageId, request, jwt));
    }

    /**
     * PATCH .../carnet-lecture/{pageId}/terminer
     * Valide et termine la page (CDL-15). Ne la rend pas publique (RG-01).
     */
    @PatchMapping("/{pageId}/terminer")
    public ResponseEntity<PageCarnetLectureResponse> terminerPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetLectureService.terminerPage(profilEnfantId, pageId, jwt));
    }

    /**
     * DELETE .../carnet-lecture/{pageId}
     */
    @DeleteMapping("/{pageId}")
    public ResponseEntity<Void> supprimerPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        carnetLectureService.supprimerPage(profilEnfantId, pageId, jwt);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST .../carnet-lecture/{pageId}/elements
     * Ajoute un personnage/lieu/objet magique/creation (CDL-04/05/06/09/11).
     */
    @PostMapping("/{pageId}/elements")
    public ResponseEntity<PageCarnetLectureResponse> ajouterElement(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @Valid @RequestBody ElementCarnetLectureRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carnetLectureService.ajouterElement(profilEnfantId, pageId, request, jwt));
    }

    /**
     * DELETE .../carnet-lecture/{pageId}/elements/{elementId}
     */
    @DeleteMapping("/{pageId}/elements/{elementId}")
    public ResponseEntity<PageCarnetLectureResponse> supprimerElement(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @PathVariable UUID elementId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetLectureService.supprimerElement(profilEnfantId, pageId, elementId, jwt));
    }
}
