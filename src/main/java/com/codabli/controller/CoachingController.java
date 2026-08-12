package com.codabli.controller;

import com.codabli.dto.OffreCoachingRequest;
import com.codabli.dto.OffreCoachingResponse;
import com.codabli.dto.ReservationCoachingRequest;
import com.codabli.dto.ReservationCoachingResponse;
import com.codabli.entity.enums.StatutReservationCoaching;
import com.codabli.service.CoachingService;
import jakarta.validation.Valid;
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
 * Controller pour le coaching (CDC PRO-07).
 *
 * Securite :
 * - GET offres : public
 * - POST/PUT/DELETE offres : admin/super_admin
 * - Reservation/mes-reservations : utilisateur authentifie
 * - Changement de statut : proprietaire (annulation) ou admin/super_admin
 * (confirmation/cloture) — verifie dans le service
 */
@RestController
@RequestMapping("/api/coaching")
public class CoachingController {

    private final CoachingService coachingService;

    public CoachingController(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    @GetMapping("/offres")
    public ResponseEntity<List<OffreCoachingResponse>> listerOffres() {
        return ResponseEntity.ok(coachingService.listerOffres());
    }

    @PostMapping("/offres")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<OffreCoachingResponse> creerOffre(@Valid @RequestBody OffreCoachingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachingService.creerOffre(request));
    }

    @PutMapping("/offres/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<OffreCoachingResponse> modifierOffre(@PathVariable UUID id,
            @Valid @RequestBody OffreCoachingRequest request) {
        return ResponseEntity.ok(coachingService.modifierOffre(id, request));
    }

    @DeleteMapping("/offres/{id}")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Void> supprimerOffre(@PathVariable UUID id) {
        coachingService.supprimerOffre(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationCoachingResponse> reserver(
            @Valid @RequestBody ReservationCoachingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachingService.reserver(request, jwt));
    }

    @GetMapping("/reservations/mes-reservations")
    public ResponseEntity<List<ReservationCoachingResponse>> mesReservations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(coachingService.mesReservations(jwt));
    }

    /**
     * PATCH /api/coaching/reservations/{id}/statut
     * Body : { "statut": "annulee" | "confirmee" | "terminee" }
     */
    @PatchMapping("/reservations/{id}/statut")
    public ResponseEntity<ReservationCoachingResponse> changerStatut(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String statutBrut = body.get("statut");
        if (statutBrut == null || statutBrut.isBlank()) {
            throw new IllegalArgumentException("Le champ 'statut' est obligatoire");
        }
        StatutReservationCoaching statut = StatutReservationCoaching.valueOf(statutBrut);
        return ResponseEntity.ok(coachingService.changerStatut(id, statut, jwt));
    }
}
