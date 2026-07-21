package com.codabli.controller;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.service.RessourcePedagogiqueService;
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
 * REST Controller pour la Mallette Pédagogique.
 */
@RestController
@RequestMapping("/api/ressources-pedagogiques")
@Tag(name = "Mallette Pédagogique", description = "Gestion du catalogue de matériel pédagogique pour les enseignants")
public class RessourcePedagogiqueController {

    private final RessourcePedagogiqueService ressourcePedagogiqueService;

    public RessourcePedagogiqueController(RessourcePedagogiqueService ressourcePedagogiqueService) {
        this.ressourcePedagogiqueService = ressourcePedagogiqueService;
    }

    @GetMapping
    @Operation(summary = "Lister les ressources de la mallette", description = "Retourne la liste paginée et filtrée des matériels pédagogiques. Les enseignants ne voient que les ressources actives.")
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'comite_lecture')")
    public ResponseEntity<Page<RessourcePedagogiqueResponse>> lister(
            @RequestParam(required = false) TypeRessource type,
            @RequestParam(required = false) String thematique,
            @RequestParam(required = false) String niveauScolaire,
            @RequestParam(required = false) Boolean actif,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<RessourcePedagogiqueResponse> resources = ressourcePedagogiqueService.lister(
                type, thematique, niveauScolaire, actif, jwt, pageable);
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le détail d'une ressource par son ID", description = "Retourne les détails de la ressource. Bloque l'accès aux enseignants si la ressource est inactive.")
    @ApiResponse(responseCode = "200", description = "Détail de la ressource trouvé")
    @ApiResponse(responseCode = "403", description = "Accès refusé pour ressource inactive")
    @ApiResponse(responseCode = "404", description = "Ressource non trouvée")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'comite_lecture')")
    public ResponseEntity<RessourcePedagogiqueResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        RessourcePedagogiqueResponse response = ressourcePedagogiqueService.getById(id, jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle ressource (Admin / Comité)", description = "Crée un nouveau matériel pédagogique.")
    @ApiResponse(responseCode = "201", description = "Ressource créée avec succès")
    @ApiResponse(responseCode = "403", description = "Permissions insuffisantes")
    @PreAuthorize("hasAnyRole('admin', 'comite_lecture')")
    public ResponseEntity<RessourcePedagogiqueResponse> creer(
            @Valid @RequestBody RessourcePedagogiqueRequest request) {

        RessourcePedagogiqueResponse response = ressourcePedagogiqueService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Valid
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une ressource existante (Admin / Comité)", description = "Met à jour les informations d'un matériel pédagogique.")
    @ApiResponse(responseCode = "200", description = "Ressource mise à jour")
    @ApiResponse(responseCode = "403", description = "Permissions insuffisantes")
    @ApiResponse(responseCode = "404", description = "Ressource non trouvée")
    @PreAuthorize("hasAnyRole('admin', 'comite_lecture')")
    public ResponseEntity<RessourcePedagogiqueResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody RessourcePedagogiqueRequest request) {

        RessourcePedagogiqueResponse response = ressourcePedagogiqueService.modifier(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement une ressource", description = "Supprime un matériel pédagogique. Action restreinte à l'administrateur uniquement.")
    @ApiResponse(responseCode = "240", description = "Suppression réussie")
    @ApiResponse(responseCode = "403", description = "Permissions insuffisantes (comité exclu)")
    @ApiResponse(responseCode = "404", description = "Ressource non trouvée")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        ressourcePedagogiqueService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
