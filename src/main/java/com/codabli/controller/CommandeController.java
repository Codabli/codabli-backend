package com.codabli.controller;

import com.codabli.dto.ChangerStatutCommandeRequest;
import com.codabli.dto.CommandeResponse;
import com.codabli.dto.CreerCommandeRequest;
import com.codabli.service.CommandeService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pour la gestion des commandes de la boutique.
 */
@RestController
@Tag(name = "Boutique - Commandes", description = "APIs de creation et suivi des commandes (Authentifie pour l'historique et la creation, Admin pour les changements de statut)")
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @PostMapping("/api/commandes")
    @Operation(summary = "Passer une commande", description = "Transforme le panier courant de l'utilisateur connecte en commande, verifie le stock, calcule le total puis vide le panier.")
    @ApiResponse(responseCode = "201", description = "Commande creee avec succes")
    @ApiResponse(responseCode = "400", description = "Panier vide ou donnees invalides")
    @ApiResponse(responseCode = "409", description = "Stock insuffisant pour un produit physique")
    public ResponseEntity<CommandeResponse> creerCommande(
            @Valid @RequestBody CreerCommandeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CommandeResponse response = commandeService.creerCommande(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/commandes")
    @Operation(summary = "Historique des commandes", description = "Liste paginee de l'historique des commandes de l'utilisateur connecte, triee par date de commande descendante.")
    @ApiResponse(responseCode = "200", description = "Liste des commandes retournee avec succes")
    public ResponseEntity<Page<CommandeResponse>> listerCommandes(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<CommandeResponse> commandes = commandeService.listerCommandes(jwt, pageable);
        return ResponseEntity.ok(commandes);
    }

    @GetMapping("/api/commandes/{id}")
    @Operation(summary = "Detail d'une commande", description = "Permet d'obtenir les details d'une commande spécifique. L'utilisateur ne peut voir que sa propre commande, sauf s'il est admin.")
    @ApiResponse(responseCode = "200", description = "Commande trouvee")
    @ApiResponse(responseCode = "403", description = "Acces refuse - cette commande n'appartient pas a l'utilisateur")
    @ApiResponse(responseCode = "404", description = "Commande non trouvee")
    public ResponseEntity<CommandeResponse> getCommandeById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        CommandeResponse response = commandeService.getCommandeById(id, jwt);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/admin/commandes/{id}/statut")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Modifier le statut d'une commande", description = "Permet de modifier l'etat d'une commande (Admin uniquement)")
    @ApiResponse(responseCode = "200", description = "Statut mis a jour avec succes")
    @ApiResponse(responseCode = "404", description = "Commande non trouvee")
    public ResponseEntity<CommandeResponse> changerStatut(
            @PathVariable UUID id,
            @Valid @RequestBody ChangerStatutCommandeRequest request) {
        CommandeResponse response = commandeService.changerStatut(id, request.getStatut());
        return ResponseEntity.ok(response);
    }
}
