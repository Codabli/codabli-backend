package com.codabli.controller;

import com.codabli.dto.EcoleRequest;
import com.codabli.dto.EcoleResponse;
import com.codabli.service.EcoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints pour gerer les ecoles.
 * Accessibles a tout utilisateur authentifie (pas de @PreAuthorize specifique).
 */
@RestController
@RequestMapping("/api/ecoles")
public class EcoleController {

    private final EcoleService ecoleService;

    public EcoleController(EcoleService ecoleService) {
        this.ecoleService = ecoleService;
    }

    @PostMapping
    public ResponseEntity<EcoleResponse> creer(@Valid @RequestBody EcoleRequest request) {
        EcoleResponse response = ecoleService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EcoleResponse>> lister() {
        return ResponseEntity.ok(ecoleService.listerTout());
    }
}
