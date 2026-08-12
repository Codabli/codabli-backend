package com.codabli.controller;

import com.codabli.dto.StatistiquesResponse;
import com.codabli.service.StatistiquesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller pour les statistiques administrateur (CDC section 15).
 */
@RestController
@RequestMapping("/api/admin/statistiques")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    public StatistiquesController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<StatistiquesResponse> consulter() {
        return ResponseEntity.ok(statistiquesService.consulter());
    }
}
