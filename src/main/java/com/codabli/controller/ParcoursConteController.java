package com.codabli.controller;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.ParcoursContexteRequest;
import com.codabli.service.ParcoursConteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Parcours "Creer mon conte danse" (epic SCRUM-29) : travail en cours d'un
 * conte danse en brouillon, enregistre etape par etape.
 * <p>
 * - GET : etat complet du parcours (reprise d'un projet)
 * - PUT /contexte : contexte de l'ecran 1 (region, ville, espace, ingredients)
 * - PUT /{etape} : contenu JSON d'une etape (decouverte, ressources,
 * ressources-classe, univers, trame, ecriture, mise-en-scene)
 * <p>
 * Utilisateur authentifie ; verification auteur ou admin dans le service.
 */
@RestController
@RequestMapping("/api/contes-danses/{conteDanseId}/parcours")
@PreAuthorize("isAuthenticated()")
public class ParcoursConteController {

    private final ParcoursConteService parcoursConteService;

    public ParcoursConteController(ParcoursConteService parcoursConteService) {
        this.parcoursConteService = parcoursConteService;
    }

    @GetMapping
    public ResponseEntity<ParcoursConteResponse> lire(
            @PathVariable UUID conteDanseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(parcoursConteService.lire(conteDanseId, jwt));
    }

    @PutMapping("/contexte")
    public ResponseEntity<ParcoursConteResponse> enregistrerContexte(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody ParcoursContexteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(parcoursConteService.enregistrerContexte(conteDanseId, request, jwt));
    }

    @PutMapping("/{etape}")
    public ResponseEntity<ParcoursConteResponse> enregistrerEtape(
            @PathVariable UUID conteDanseId,
            @PathVariable String etape,
            @RequestBody Map<String, Object> contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(parcoursConteService.enregistrerEtape(conteDanseId, etape, contenu, jwt));
    }
}
