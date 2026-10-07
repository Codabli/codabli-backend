package com.codabli.controller;

import com.codabli.dto.ProjetConteRequest;
import com.codabli.dto.ProjetConteResponse;
import com.codabli.service.ProjetConteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Controller des projets de conte danse (parcours "Creer mon conte").
 * <p>
 * Securite a 2 niveaux :
 * 1. @PreAuthorize → reserve aux enseignants, professionnels de l'education et admins.
 * 2. Service → un enseignant n'accede qu'a ses propres projets.
 */
@RestController
@RequestMapping("/api/projets-contes")
@PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin')")
@Tag(name = "Creer mon conte - Projets", description = "APIs du projet de conte danse de l'enseignant connecte")
public class ProjetConteController {

    private final ProjetConteService projetConteService;

    public ProjetConteController(ProjetConteService projetConteService) {
        this.projetConteService = projetConteService;
    }

    @GetMapping
    @Operation(summary = "Lister mes projets de conte", description = "Liste les projets de l'enseignant connecte, du plus recemment modifie au plus ancien")
    @ApiResponse(responseCode = "200", description = "Liste des projets retournee avec succes")
    public ResponseEntity<List<ProjetConteResponse>> lister(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(projetConteService.listerMesProjets(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un projet de conte")
    @ApiResponse(responseCode = "200", description = "Projet retourne avec succes")
    @ApiResponse(responseCode = "403", description = "Acces refuse - ce projet n'appartient pas a l'utilisateur")
    @ApiResponse(responseCode = "404", description = "Projet non trouve")
    public ResponseEntity<ProjetConteResponse> consulter(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(projetConteService.consulter(id, jwt));
    }

    @PostMapping
    @Operation(summary = "Creer un projet de conte", description = "Cree un projet avec le contexte saisi a l'ecran 1 (RG-CMC-01, RG-CMC-02)")
    @ApiResponse(responseCode = "201", description = "Projet cree avec succes")
    @ApiResponse(responseCode = "400", description = "Donnees invalides")
    public ResponseEntity<ProjetConteResponse> creer(
            @Valid @RequestBody ProjetConteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProjetConteResponse response = projetConteService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier le contexte d'un projet de conte")
    @ApiResponse(responseCode = "200", description = "Contexte modifie avec succes")
    @ApiResponse(responseCode = "400", description = "Donnees invalides")
    @ApiResponse(responseCode = "403", description = "Acces refuse - ce projet n'appartient pas a l'utilisateur")
    @ApiResponse(responseCode = "404", description = "Projet non trouve")
    public ResponseEntity<ProjetConteResponse> modifierContexte(
            @PathVariable UUID id,
            @Valid @RequestBody ProjetConteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(projetConteService.modifierContexte(id, request, jwt));
    }
}
