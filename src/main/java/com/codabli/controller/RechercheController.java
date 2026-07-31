package com.codabli.controller;

import com.codabli.dto.ActualiteResponse;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.dto.GalerieItemResponse;
import com.codabli.dto.PartenaireResponse;
import com.codabli.dto.ProduitResponse;
import com.codabli.service.ActualiteService;
import com.codabli.service.ConteDanseService;
import com.codabli.service.GalerieService;
import com.codabli.service.PartenaireService;
import com.codabli.service.ProduitService;
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
 * Endpoint de recherche transversale (CDC 8.19, REC-01/02).
 * Interroge les contenus publics : actualites, contes danses, cartes a
 * conte validees, produits et partenaires actifs.
 *
 * Les ressources pedagogiques et fiches d'activites sont volontairement
 * exclues : leur acces est reserve a certains roles (enseignant,
 * professionnel_education, admin, comite_lecture) et les exposer dans une
 * recherche publique reviendrait a contourner cette restriction.
 *
 * Public, aucune authentification requise.
 */
@RestController
@RequestMapping("/api/recherche")
public class RechercheController {

    private final ActualiteService actualiteService;
    private final ConteDanseService conteDanseService;
    private final GalerieService galerieService;
    private final ProduitService produitService;
    private final PartenaireService partenaireService;

    public RechercheController(ActualiteService actualiteService,
            ConteDanseService conteDanseService,
            GalerieService galerieService,
            ProduitService produitService,
            PartenaireService partenaireService) {
        this.actualiteService = actualiteService;
        this.conteDanseService = conteDanseService;
        this.galerieService = galerieService;
        this.produitService = produitService;
        this.partenaireService = partenaireService;
    }

    /**
     * GET /api/recherche?q=mot-cle
     * Recherche transversale dans les contenus publics.
     * Retourne : { actualites, contesDanses, cartesAConte, produits,
     * partenaires }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> rechercher(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ActualiteResponse> actualites = actualiteService.rechercher(q, pageable);
        Page<ConteDanseResponse> contesDanses = conteDanseService.rechercher(q, pageable);
        Page<GalerieItemResponse> cartesAConte = galerieService.rechercher(q, pageable);
        Page<ProduitResponse> produits = produitService.rechercher(q, pageable);
        Page<PartenaireResponse> partenaires = partenaireService.rechercher(q, pageable);

        Map<String, Object> resultats = new LinkedHashMap<>();
        resultats.put("actualites", actualites);
        resultats.put("contesDanses", contesDanses);
        resultats.put("cartesAConte", cartesAConte);
        resultats.put("produits", produits);
        resultats.put("partenaires", partenaires);

        return ResponseEntity.ok(resultats);
    }
}
