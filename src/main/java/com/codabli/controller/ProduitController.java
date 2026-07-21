package com.codabli.controller;

import com.codabli.dto.ProduitRequest;
import com.codabli.dto.ProduitResponse;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.service.ProduitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Controller pour la gestion des produits de la boutique.
 *
 * Securite :
 * - Lecture (GET) : publique
 * - Ecriture (POST/PUT/DELETE) : reservee aux admins
 */
@RestController
@RequestMapping("/api/produits")
@Tag(name = "Boutique - Produits", description = "APIs de gestion des produits de l'application (Public en lecture, Admin en modification)")
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(ProduitService produitService) {
        this.produitService = produitService;
    }

    @GetMapping
    @Operation(summary = "Lister les produits", description = "Permet de lister les produits avec pagination et filtrage par type et actif (Public)")
    @ApiResponse(responseCode = "200", description = "Liste des produits retournee avec succes")
    public ResponseEntity<Page<ProduitResponse>> lister(
            @RequestParam(required = false) TypeProduit type,
            @RequestParam(required = false) Boolean actif,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ProduitResponse> produits = produitService.lister(type, actif, pageable);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le detail d'un produit", description = "Permet de recuperer un produit par son identifiant unique (Public)")
    @ApiResponse(responseCode = "200", description = "Produit trouve")
    @ApiResponse(responseCode = "404", description = "Produit non trouve")
    public ResponseEntity<ProduitResponse> getById(@PathVariable UUID id) {
        ProduitResponse response = produitService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Creer un produit", description = "Ajoute un nouveau produit au catalogue (Admin uniquement)")
    @ApiResponse(responseCode = "201", description = "Produit cree avec succes")
    @ApiResponse(responseCode = "400", description = "Requete invalide")
    public ResponseEntity<ProduitResponse> creer(@Valid @RequestBody ProduitRequest request) {
        ProduitResponse response = produitService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Modifier un produit", description = "Modifie les informations d'un produit existant (Admin uniquement)")
    @ApiResponse(responseCode = "200", description = "Produit modifie avec succes")
    @ApiResponse(responseCode = "404", description = "Produit non trouve")
    public ResponseEntity<ProduitResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ProduitRequest request) {
        ProduitResponse response = produitService.modifier(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Supprimer un produit", description = "Supprime un produit du catalogue (Admin uniquement)")
    @ApiResponse(responseCode = "204", description = "Produit supprime avec succes")
    @ApiResponse(responseCode = "404", description = "Produit non trouve")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        produitService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
