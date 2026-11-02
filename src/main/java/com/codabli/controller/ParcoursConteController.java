package com.codabli.controller;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.ParcoursContexteRequest;
import com.codabli.dto.parcours.ContenuEtapeParcours;
import com.codabli.dto.parcours.DecouverteParcours;
import com.codabli.dto.parcours.EcritureParcours;
import com.codabli.dto.parcours.MiseEnSceneParcours;
import com.codabli.dto.parcours.RessourcesClasseParcours;
import com.codabli.dto.parcours.RessourcesParcours;
import com.codabli.dto.parcours.TrameParcours;
import com.codabli.dto.parcours.UniversParcours;
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

import java.util.UUID;

/**
 * Parcours "Creer mon conte danse" (epic SCRUM-29) : travail en cours d'un
 * conte danse en brouillon, enregistre etape par etape.
 * <p>
 * - GET : etat complet du parcours (reprise d'un projet)
 * - PUT /contexte : contexte de l'ecran 1 (region, ville, espace, ingredients)
 * - PUT /decouverte, /ressources, /ressources-classe, /univers, /trame,
 * /ecriture, /mise-en-scene : contenu d'une etape (dto.parcours), qui
 * remplace le precedent
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

    @PutMapping("/decouverte")
    public ResponseEntity<ParcoursConteResponse> enregistrerDecouverte(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody DecouverteParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/ressources")
    public ResponseEntity<ParcoursConteResponse> enregistrerRessources(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody RessourcesParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/ressources-classe")
    public ResponseEntity<ParcoursConteResponse> enregistrerRessourcesClasse(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody RessourcesClasseParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/univers")
    public ResponseEntity<ParcoursConteResponse> enregistrerUnivers(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody UniversParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/trame")
    public ResponseEntity<ParcoursConteResponse> enregistrerTrame(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody TrameParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/ecriture")
    public ResponseEntity<ParcoursConteResponse> enregistrerEcriture(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody EcritureParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    @PutMapping("/mise-en-scene")
    public ResponseEntity<ParcoursConteResponse> enregistrerMiseEnScene(
            @PathVariable UUID conteDanseId,
            @Valid @RequestBody MiseEnSceneParcours contenu,
            @AuthenticationPrincipal Jwt jwt) {
        return enregistrerEtape(conteDanseId, contenu, jwt);
    }

    private ResponseEntity<ParcoursConteResponse> enregistrerEtape(UUID conteDanseId, ContenuEtapeParcours contenu,
                                                                   Jwt jwt) {
        return ResponseEntity.ok(parcoursConteService.enregistrerEtape(conteDanseId, contenu, jwt));
    }
}
