package com.codabli.controller;

import com.codabli.dto.ElementCarnetVoyageRequest;
import com.codabli.dto.PageCarnetVoyageRequest;
import com.codabli.dto.PageCarnetVoyageResponse;
import com.codabli.service.CarnetVoyageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour le Carnet de Voyage (CDC 8.17.2, CDV-01 a 14).
 * Meme modele de securite que le Carnet de Lecture (RG-13).
 */
@RestController
@RequestMapping("/api/profils-enfants/{profilEnfantId}/carnet-voyage")
public class CarnetVoyageController {

    private final CarnetVoyageService carnetVoyageService;

    public CarnetVoyageController(CarnetVoyageService carnetVoyageService) {
        this.carnetVoyageService = carnetVoyageService;
    }

    @PostMapping
    public ResponseEntity<PageCarnetVoyageResponse> creerPage(
            @PathVariable UUID profilEnfantId,
            @RequestBody PageCarnetVoyageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        PageCarnetVoyageResponse response = carnetVoyageService.creerPage(profilEnfantId, request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PageCarnetVoyageResponse>> listerPages(
            @PathVariable UUID profilEnfantId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetVoyageService.listerPages(profilEnfantId, jwt));
    }

    @GetMapping("/{pageId}")
    public ResponseEntity<PageCarnetVoyageResponse> getPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetVoyageService.getPage(profilEnfantId, pageId, jwt));
    }

    @PutMapping("/{pageId}")
    public ResponseEntity<PageCarnetVoyageResponse> modifierPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @RequestBody PageCarnetVoyageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetVoyageService.modifierPage(profilEnfantId, pageId, request, jwt));
    }

    @PatchMapping("/{pageId}/terminer")
    public ResponseEntity<PageCarnetVoyageResponse> terminerPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetVoyageService.terminerPage(profilEnfantId, pageId, jwt));
    }

    @DeleteMapping("/{pageId}")
    public ResponseEntity<Void> supprimerPage(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal Jwt jwt) {
        carnetVoyageService.supprimerPage(profilEnfantId, pageId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pageId}/elements")
    public ResponseEntity<PageCarnetVoyageResponse> ajouterElement(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @Valid @RequestBody ElementCarnetVoyageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carnetVoyageService.ajouterElement(profilEnfantId, pageId, request, jwt));
    }

    @DeleteMapping("/{pageId}/elements/{elementId}")
    public ResponseEntity<PageCarnetVoyageResponse> supprimerElement(
            @PathVariable UUID profilEnfantId,
            @PathVariable UUID pageId,
            @PathVariable UUID elementId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carnetVoyageService.supprimerElement(profilEnfantId, pageId, elementId, jwt));
    }
}
