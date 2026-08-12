package com.codabli.controller;

import com.codabli.dto.EvaluationPedagogiqueRequest;
import com.codabli.dto.EvaluationPedagogiqueResponse;
import com.codabli.service.EvaluationPedagogiqueService;
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
 * Controller pour les evaluations pedagogiques (CDC PRO-05).
 *
 * Securite : reserve aux enseignants/professionnels de l'education pour la
 * creation ; consultation/modification/suppression limitees a l'auteur (ou
 * admin/super_admin), verifie dans le service.
 */
@RestController
@RequestMapping("/api/evaluations-pedagogiques")
public class EvaluationPedagogiqueController {

    private final EvaluationPedagogiqueService evaluationPedagogiqueService;

    public EvaluationPedagogiqueController(EvaluationPedagogiqueService evaluationPedagogiqueService) {
        this.evaluationPedagogiqueService = evaluationPedagogiqueService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin')")
    public ResponseEntity<EvaluationPedagogiqueResponse> creer(
            @Valid @RequestBody EvaluationPedagogiqueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationPedagogiqueService.creer(request, jwt));
    }

    @GetMapping("/mes-evaluations")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin')")
    public ResponseEntity<List<EvaluationPedagogiqueResponse>> mesEvaluations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(evaluationPedagogiqueService.mesEvaluations(jwt));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin')")
    public ResponseEntity<EvaluationPedagogiqueResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(evaluationPedagogiqueService.getById(id, jwt));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin')")
    public ResponseEntity<EvaluationPedagogiqueResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluationPedagogiqueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(evaluationPedagogiqueService.modifier(id, request, jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('enseignant', 'professionnel_education', 'admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        evaluationPedagogiqueService.supprimer(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
