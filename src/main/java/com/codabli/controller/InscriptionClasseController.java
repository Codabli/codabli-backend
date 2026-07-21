package com.codabli.controller;

import com.codabli.dto.InscriptionClasseRequest;
import com.codabli.dto.InscriptionClasseResponse;
import com.codabli.service.InscriptionClasseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint pour inscrire un eleve dans une classe.
 */
@RestController
@RequestMapping("/api/inscriptions-classes")
public class InscriptionClasseController {

    private final InscriptionClasseService inscriptionClasseService;

    public InscriptionClasseController(InscriptionClasseService inscriptionClasseService) {
        this.inscriptionClasseService = inscriptionClasseService;
    }

    @PostMapping
    public ResponseEntity<InscriptionClasseResponse> inscrire(
            @Valid @RequestBody InscriptionClasseRequest request) {
        InscriptionClasseResponse response = inscriptionClasseService.inscrire(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
