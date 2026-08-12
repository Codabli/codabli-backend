package com.codabli.controller;

import com.codabli.dto.MalletteArtistesResponse;
import com.codabli.service.MalletteArtistesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller pour la Mallette d'Artistes (CDC 8.16), vue agregee de
 * l'espace creatif d'un profil enfant.
 */
@RestController
@RequestMapping("/api/profils-enfants/{profilEnfantId}/mallette-artistes")
public class MalletteArtistesController {

    private final MalletteArtistesService malletteArtistesService;

    public MalletteArtistesController(MalletteArtistesService malletteArtistesService) {
        this.malletteArtistesService = malletteArtistesService;
    }

    @GetMapping
    public ResponseEntity<MalletteArtistesResponse> consulter(
            @PathVariable UUID profilEnfantId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(malletteArtistesService.consulter(profilEnfantId, jwt));
    }
}
