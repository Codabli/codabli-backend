package com.codabli.controller;

import com.codabli.dto.ActualiteResponse;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.service.ActualiteService;
import com.codabli.service.ConteDanseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de recherche transversale.
 * Interroge a la fois les actualites publiees et les contes danses publies.
 *
 * Public, aucune authentification requise.
 */
@RestController
@RequestMapping("/api/recherche")
public class RechercheController {

    private final ActualiteService actualiteService;
    private final ConteDanseService conteDanseService;

    public RechercheController(ActualiteService actualiteService,
            ConteDanseService conteDanseService) {
        this.actualiteService = actualiteService;
        this.conteDanseService = conteDanseService;
    }

    /**
     * GET /api/recherche?q=mot-cle
     * Recherche dans les actualites publiees et les contes danses publies.
     * Retourne un objet combine : { actualites: Page, contesDanses: Page }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> rechercher(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ActualiteResponse> actualites = actualiteService.rechercher(q, pageable);
        Page<ConteDanseResponse> contesDanses = conteDanseService.rechercher(q, pageable);

        Map<String, Object> resultats = new LinkedHashMap<>();
        resultats.put("actualites", actualites);
        resultats.put("contesDanses", contesDanses);

        return ResponseEntity.ok(resultats);
    }
}
