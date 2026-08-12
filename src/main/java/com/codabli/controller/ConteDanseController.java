package com.codabli.controller;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.entity.enums.AccesConte;
import com.codabli.service.ConteDanseService;
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
import java.util.Map;
import java.util.UUID;

/**
 * Controller pour les contes danses.
 *
 * Securite a deux niveaux :
 * 1. @PreAuthorize → verifie le role (grossier)
 * 2. Le service → verifie la relation auteur/admin (fin)
 *
 * Acces :
 * - GET (catalogue) : public, aucune authentification requise
 * - POST : utilisateur authentifie
 * - PUT / DELETE : utilisateur authentifie + verification auteur ou admin dans
 * le service
 */
@RestController
@RequestMapping("/api/contes-danses")
public class ConteDanseController {

    private final ConteDanseService conteDanseService;

    public ConteDanseController(ConteDanseService conteDanseService) {
        this.conteDanseService = conteDanseService;
    }

    /**
     * GET /api/contes-danses
     * Catalogue public des contes publies, pagine (CON-01).
     * Filtres optionnels (CON-03) : ?langue=fr&pays=Liban&thematique=amitie&acces=gratuit&age=8
     * Parametres : ?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<ConteDanseResponse>> lister(
            @RequestParam(required = false) String langue,
            @RequestParam(required = false) String pays,
            @RequestParam(required = false) String thematique,
            @RequestParam(required = false) AccesConte acces,
            @RequestParam(required = false) Integer age,
            @PageableDefault(size = 10) Pageable pageable) {
        boolean aucunFiltre = langue == null && pays == null && thematique == null && acces == null && age == null;
        Page<ConteDanseResponse> contes = aucunFiltre
                ? conteDanseService.listerPublies(pageable)
                : conteDanseService.filtrerPublies(langue, pays, thematique, acces, age, pageable);
        return ResponseEntity.ok(contes);
    }

    /**
     * GET /api/contes-danses/{id}
     * Detail d'un conte danse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConteDanseResponse> getById(@PathVariable UUID id) {
        ConteDanseResponse response = conteDanseService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/contes-danses
     * Cree un conte danse. Tout utilisateur authentifie peut creer.
     * Le createur est identifie via le JWT, pas via le body.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteDanseResponse> creer(
            @Valid @RequestBody ConteDanseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ConteDanseResponse response = conteDanseService.creer(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/contes-danses/{id}
     * Modifie un conte danse. L'auteur ou un admin peut modifier.
     * La verification fine est faite dans le service.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteDanseResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ConteDanseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ConteDanseResponse response = conteDanseService.modifier(id, request, jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/contes-danses/{id}
     * Supprime un conte danse. L'auteur ou un admin peut supprimer.
     * La verification fine est faite dans le service.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        conteDanseService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/contes-danses/mes-contes
     * Retrouve les contes de l'auteur connecte, quel que soit leur statut
     * (brouillon, en revision, publie, refuse).
     */
    @GetMapping("/mes-contes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConteDanseResponse>> mesContes(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(conteDanseService.mesContes(jwt));
    }

    /**
     * GET /api/contes-danses/en-attente
     * File d'attente de moderation, filtree selon le role de l'appelant
     * (enseignant : ses classes ; comite_lecture : en revision comite ;
     * admin/super_admin : tout ce qui est en revision).
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('enseignant', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<List<ConteDanseResponse>> listerEnAttente(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(conteDanseService.listerEnAttente(jwt));
    }

    /**
     * PATCH /api/contes-danses/{id}/soumettre
     * L'auteur soumet son brouillon (ou son conte refuse corrige) a la
     * moderation.
     */
    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteDanseResponse> soumettre(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(conteDanseService.soumettre(id, jwt));
    }

    /**
     * PATCH /api/contes-danses/{id}/valider
     * Fait progresser le conte dans le cycle de validation (enseignant ->
     * comite de lecture -> publie). admin/super_admin publient directement.
     */
    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('enseignant', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<ConteDanseResponse> valider(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(conteDanseService.valider(id, jwt));
    }

    /**
     * PATCH /api/contes-danses/{id}/refuser
     * Refuse le conte. Corps optionnel : { "motif": "..." }
     */
    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('enseignant', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<ConteDanseResponse> refuser(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String motif = body != null ? body.get("motif") : null;
        return ResponseEntity.ok(conteDanseService.refuser(id, motif, jwt));
    }
}
