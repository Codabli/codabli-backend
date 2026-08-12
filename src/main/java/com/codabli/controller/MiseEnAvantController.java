package com.codabli.controller;

import com.codabli.dto.MiseEnAvantRequest;
import com.codabli.dto.MiseEnAvantResponse;
import com.codabli.service.MiseEnAvantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller pour les mises en avant de la page d'accueil (CDC ACC-04).
 *
 * Securite : GET public (uniquement les mises en avant actives et dans leur
 * fenetre de diffusion) ; gestion reservee admin/super_admin.
 */
@RestController
@RequestMapping("/api/mise-en-avant")
public class MiseEnAvantController {

    private final MiseEnAvantService miseEnAvantService;

    public MiseEnAvantController(MiseEnAvantService miseEnAvantService) {
        this.miseEnAvantService = miseEnAvantService;
    }

    @GetMapping
    public ResponseEntity<List<MiseEnAvantResponse>> listerActives() {
        return ResponseEntity.ok(miseEnAvantService.listerActives());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<MiseEnAvantResponse> creer(@Valid @RequestBody MiseEnAvantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(miseEnAvantService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<MiseEnAvantResponse> modifier(@PathVariable UUID id,
            @Valid @RequestBody MiseEnAvantRequest request) {
        return ResponseEntity.ok(miseEnAvantService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        miseEnAvantService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
