package com.codabli.controller;

import com.codabli.dto.ConteDanseTraductionRequest;
import com.codabli.dto.ConteDanseTraductionResponse;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.service.ConteDanseTraductionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller pour les traductions de Contes Danses (CDC 8.5, LAN-01/02/03).
 *
 * Securite :
 * - GET /traductions (public) : uniquement les traductions publiees
 * - GET /traductions/gestion + POST/PUT/PATCH/DELETE : reserves aux
 * traducteurs, au comite de lecture et aux admins
 */
@RestController
@RequestMapping("/api/contes-danses/{conteId}/traductions")
public class ConteDanseTraductionController {

    private final ConteDanseTraductionService traductionService;

    public ConteDanseTraductionController(ConteDanseTraductionService traductionService) {
        this.traductionService = traductionService;
    }

    /**
     * GET /api/contes-danses/{conteId}/traductions
     * Langues disponibles publiquement pour ce conte (LAN-01).
     */
    @GetMapping
    public ResponseEntity<List<ConteDanseTraductionResponse>> listerPubliees(@PathVariable UUID conteId) {
        return ResponseEntity.ok(traductionService.listerPubliees(conteId));
    }

    /**
     * GET /api/contes-danses/{conteId}/traductions/gestion
     * Toutes les traductions, quel que soit leur statut (LAN-02).
     */
    @GetMapping("/gestion")
    @PreAuthorize("hasAnyRole('traducteur', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<List<ConteDanseTraductionResponse>> listerToutes(@PathVariable UUID conteId) {
        return ResponseEntity.ok(traductionService.listerToutes(conteId));
    }

    /**
     * POST /api/contes-danses/{conteId}/traductions
     * Cree une traduction.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('traducteur', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<ConteDanseTraductionResponse> creer(
            @PathVariable UUID conteId,
            @Valid @RequestBody ConteDanseTraductionRequest request) {
        ConteDanseTraductionResponse response = traductionService.creer(conteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/contes-danses/{conteId}/traductions/{id}
     * Modifie le contenu d'une traduction.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('traducteur', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<ConteDanseTraductionResponse> modifier(
            @PathVariable UUID conteId,
            @PathVariable UUID id,
            @Valid @RequestBody ConteDanseTraductionRequest request) {
        return ResponseEntity.ok(traductionService.modifier(id, request));
    }

    /**
     * PATCH /api/contes-danses/{conteId}/traductions/{id}/statut
     * Change le statut d'une traduction (LAN-02). Body : { "statut": "validee" }
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('traducteur', 'comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<ConteDanseTraductionResponse> changerStatut(
            @PathVariable UUID conteId,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String statutBrut = body.get("statut");
        if (statutBrut == null || statutBrut.isBlank()) {
            throw new IllegalArgumentException("Le champ 'statut' est obligatoire");
        }
        StatutTraduction statut = StatutTraduction.valueOf(statutBrut);
        return ResponseEntity.ok(traductionService.changerStatut(id, statut));
    }

    /**
     * DELETE /api/contes-danses/{conteId}/traductions/{id}
     * Reserve au comite de lecture et aux admins.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('comite_lecture', 'admin', 'super_admin')")
    public ResponseEntity<Void> supprimer(
            @PathVariable UUID conteId,
            @PathVariable UUID id) {
        traductionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
