package com.codabli.controller;

import com.codabli.dto.AdresseLivraisonRequest;
import com.codabli.dto.AdresseLivraisonResponse;
import com.codabli.service.AdresseLivraisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour la gestion des adresses de livraison de l'utilisateur
 * connecte.
 */
@RestController
@RequestMapping("/api/adresses-livraison")
@Tag(name = "Boutique - Adresses", description = "APIs de gestion des adresses de livraison associees a l'utilisateur connecte")
public class AdresseLivraisonController {

    private final AdresseLivraisonService adresseLivraisonService;

    public AdresseLivraisonController(AdresseLivraisonService adresseLivraisonService) {
        this.adresseLivraisonService = adresseLivraisonService;
    }

    @GetMapping
    @Operation(summary = "Lister les adresses de livraison", description = "Liste toutes les adresses enregistrees par l'utilisateur connecte")
    @ApiResponse(responseCode = "200", description = "Liste des adresses retournee avec succes")
    public ResponseEntity<List<AdresseLivraisonResponse>> lister(@AuthenticationPrincipal Jwt jwt) {
        List<AdresseLivraisonResponse> adresses = adresseLivraisonService.lister(jwt);
        return ResponseEntity.ok(adresses);
    }

    @PostMapping
    @Operation(summary = "Ajouter une adresse de livraison", description = "Enregistre une nouvelle adresse de livraison pour l'utilisateur connecte")
    @ApiResponse(responseCode = "201", description = "Adresse creee avec succes")
    @ApiResponse(responseCode = "400", description = "Donnees invalides")
    public ResponseEntity<AdresseLivraisonResponse> creer(
            @Valid @RequestBody AdresseLivraisonRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AdresseLivraisonResponse response = adresseLivraisonService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une adresse de livraison", description = "Modifie une adresse existante appartenant a l'utilisateur connecte")
    @ApiResponse(responseCode = "200", description = "Adresse modifiee avec succes")
    @ApiResponse(responseCode = "403", description = "Acces refuse - cette adresse n'appartient pas a l'utilisateur")
    @ApiResponse(responseCode = "404", description = "Adresse non trouvee")
    public ResponseEntity<AdresseLivraisonResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody AdresseLivraisonRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AdresseLivraisonResponse response = adresseLivraisonService.modifier(id, request, jwt);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une adresse de livraison", description = "Supprime une adresse existante appartenant a l'utilisateur connecte")
    @ApiResponse(responseCode = "204", description = "Adresse supprimee avec succes")
    @ApiResponse(responseCode = "403", description = "Acces refuse - cette adresse n'appartient pas a l'utilisateur")
    @ApiResponse(responseCode = "404", description = "Adresse non trouvee")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        adresseLivraisonService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
