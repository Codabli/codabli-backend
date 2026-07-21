package com.codabli.controller;

import com.codabli.dto.AjoutPanierRequest;
import com.codabli.dto.ModifQuantiteRequest;
import com.codabli.dto.PanierResponse;
import com.codabli.service.PanierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pour la gestion du panier de l'utilisateur connecte.
 *
 * Securite :
 * - Tous les endpoints requierent une authentification valide.
 */
@RestController
@RequestMapping("/api/panier")
@Tag(name = "Boutique - Panier", description = "APIs de gestion du panier de l'utilisateur connecte (Authentifie uniquement)")
public class PanierController {

    private final PanierService panierService;

    public PanierController(PanierService panierService) {
        this.panierService = panierService;
    }

    @GetMapping
    @Operation(summary = "Consulter le panier", description = "Recupere le panier de l'utilisateur connecte. Cree un panier vide s'il n'existe pas encore.")
    @ApiResponse(responseCode = "200", description = "Panier recupere avec succes")
    public ResponseEntity<PanierResponse> getPanier(@AuthenticationPrincipal Jwt jwt) {
        PanierResponse panier = panierService.getPanier(jwt);
        return ResponseEntity.ok(panier);
    }

    @PostMapping("/items")
    @Operation(summary = "Ajouter un produit", description = "Ajoute un produit au panier ou augmente sa quantite si deja existant")
    @ApiResponse(responseCode = "200", description = "Produit ajoute et panier mis a jour")
    @ApiResponse(responseCode = "404", description = "Produit non trouve")
    public ResponseEntity<PanierResponse> ajouterProduit(
            @Valid @RequestBody AjoutPanierRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        PanierResponse panier = panierService.ajouterProduit(request, jwt);
        return ResponseEntity.ok(panier);
    }

    @PatchMapping("/items/{ligneId}")
    @Operation(summary = "Modifier la quantite d'une ligne", description = "Permet de modifier directement la quantite associee a une ligne du panier")
    @ApiResponse(responseCode = "200", description = "Quantite modifiee avec succes")
    @ApiResponse(responseCode = "440", description = "Ligne de panier non trouvee")
    public ResponseEntity<PanierResponse> modifierQuantite(
            @PathVariable UUID ligneId,
            @Valid @RequestBody ModifQuantiteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        PanierResponse panier = panierService.modifierQuantite(ligneId, request, jwt);
        return ResponseEntity.ok(panier);
    }

    @DeleteMapping("/items/{ligneId}")
    @Operation(summary = "Supprimer une ligne du panier", description = "Retire completement une ligne de produits du panier de l'utilisateur")
    @ApiResponse(responseCode = "200", description = "Ligne supprimee avec succes")
    @ApiResponse(responseCode = "404", description = "Ligne non trouvee")
    public ResponseEntity<PanierResponse> supprimerLigne(
            @PathVariable UUID ligneId,
            @AuthenticationPrincipal Jwt jwt) {
        PanierResponse panier = panierService.supprimerLigne(ligneId, jwt);
        return ResponseEntity.ok(panier);
    }

    @DeleteMapping
    @Operation(summary = "Vider le panier", description = "Supprime toutes les lignes/produits presents dans le panier de l'utilisateur")
    @ApiResponse(responseCode = "204", description = "Panier vide avec succes")
    public ResponseEntity<Void> viderPanier(@AuthenticationPrincipal Jwt jwt) {
        panierService.viderPanier(jwt);
        return ResponseEntity.noContent().build();
    }
}
