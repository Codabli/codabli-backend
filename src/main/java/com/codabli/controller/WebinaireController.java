package com.codabli.controller;

import com.codabli.dto.InscriptionWebinaireResponse;
import com.codabli.dto.WebinaireRequest;
import com.codabli.dto.WebinaireResponse;
import com.codabli.service.WebinaireService;
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

import java.util.List;
import java.util.UUID;

/**
 * Controller pour les webinaires (CDC PRO-06).
 *
 * Securite :
 * - GET (catalogue) : public
 * - POST/PUT/DELETE : admin/super_admin
 * - Inscription/desinscription/mes-inscriptions : utilisateur authentifie
 */
@RestController
@RequestMapping("/api/webinaires")
public class WebinaireController {

    private final WebinaireService webinaireService;

    public WebinaireController(WebinaireService webinaireService) {
        this.webinaireService = webinaireService;
    }

    @GetMapping
    public ResponseEntity<Page<WebinaireResponse>> lister(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(webinaireService.lister(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebinaireResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(webinaireService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<WebinaireResponse> creer(@Valid @RequestBody WebinaireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webinaireService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<WebinaireResponse> modifier(@PathVariable UUID id, @Valid @RequestBody WebinaireRequest request) {
        return ResponseEntity.ok(webinaireService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        webinaireService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inscriptions")
    public ResponseEntity<InscriptionWebinaireResponse> sInscrire(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webinaireService.sInscrire(id, jwt));
    }

    @DeleteMapping("/{id}/inscriptions")
    public ResponseEntity<Void> seDesinscrire(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        webinaireService.seDesinscrire(id, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mes-inscriptions")
    public ResponseEntity<List<InscriptionWebinaireResponse>> mesInscriptions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(webinaireService.mesInscriptions(jwt));
    }
}
