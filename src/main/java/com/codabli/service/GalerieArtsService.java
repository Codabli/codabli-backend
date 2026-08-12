package com.codabli.service;

import com.codabli.dto.FresqueRequest;
import com.codabli.dto.FresqueResponse;
import com.codabli.dto.HotspotRequest;
import com.codabli.dto.HotspotResponse;
import com.codabli.dto.SalleGalerieRequest;
import com.codabli.dto.SalleGalerieResponse;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.Fresque;
import com.codabli.entity.Hotspot;
import com.codabli.entity.SalleGalerie;
import com.codabli.repository.FresqueRepository;
import com.codabli.repository.SalleGalerieRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour le socle structurel de la Galerie des Arts immersive
 * (CDC 8.11) : hall (salles/pays), fresques principales et hotspots.
 *
 * Perimetre volontairement limite au contenu structurel gere par
 * l'administration ; la navigation immersive (2,5D), la collecte
 * d'autocollants et les mini-jeux d'observation relevent du frontend et/ou
 * de la gamification (hors perimetre de cette passe).
 */
@Service
@Transactional
public class GalerieArtsService {

    private final SalleGalerieRepository salleGalerieRepository;
    private final FresqueRepository fresqueRepository;
    private final EntityManager entityManager;

    public GalerieArtsService(SalleGalerieRepository salleGalerieRepository,
            FresqueRepository fresqueRepository,
            EntityManager entityManager) {
        this.salleGalerieRepository = salleGalerieRepository;
        this.fresqueRepository = fresqueRepository;
        this.entityManager = entityManager;
    }

    // ────────────────────────────────────────────────────────────────
    // SALLES — GAL-01/02
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalleGalerieResponse> listerSalles() {
        return salleGalerieRepository.findByActifTrueOrderByOrdreAffichageAsc().stream()
                .map(this::toSalleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalleGalerieResponse getSalle(UUID id) {
        return toSalleResponse(getSalleOrThrow(id));
    }

    public SalleGalerieResponse creerSalle(SalleGalerieRequest request) {
        SalleGalerie salle = SalleGalerie.builder()
                .pays(request.getPays())
                .introduction(request.getIntroduction())
                .ambianceSonoreUrl(request.getAmbianceSonoreUrl())
                .paletteCouleurs(request.getPaletteCouleurs())
                .ordreAffichage(request.getOrdreAffichage() != null ? request.getOrdreAffichage() : 0)
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toSalleResponse(salleGalerieRepository.save(salle));
    }

    public SalleGalerieResponse modifierSalle(UUID id, SalleGalerieRequest request) {
        SalleGalerie salle = getSalleOrThrow(id);
        salle.setPays(request.getPays());
        salle.setIntroduction(request.getIntroduction());
        salle.setAmbianceSonoreUrl(request.getAmbianceSonoreUrl());
        salle.setPaletteCouleurs(request.getPaletteCouleurs());
        if (request.getOrdreAffichage() != null) {
            salle.setOrdreAffichage(request.getOrdreAffichage());
        }
        if (request.getActif() != null) {
            salle.setActif(request.getActif());
        }
        return toSalleResponse(salleGalerieRepository.save(salle));
    }

    public void supprimerSalle(UUID id) {
        salleGalerieRepository.delete(getSalleOrThrow(id));
    }

    // ────────────────────────────────────────────────────────────────
    // FRESQUES — GAL-06/07
    // ────────────────────────────────────────────────────────────────

    public FresqueResponse creerFresque(FresqueRequest request) {
        SalleGalerie salle = getSalleOrThrow(request.getSalleId());

        if (fresqueRepository.findBySalleId(salle.getId()).isPresent()) {
            throw new IllegalStateException("Cette salle possede deja une fresque principale");
        }

        Fresque fresque = Fresque.builder()
                .salle(salle)
                .titre(request.getTitre())
                .imageUrl(request.getImageUrl())
                .introduction(request.getIntroduction())
                .build();

        if (request.getConteId() != null) {
            fresque.setConte(entityManager.getReference(ConteDanse.class, request.getConteId()));
        }

        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    public FresqueResponse modifierFresque(UUID id, FresqueRequest request) {
        Fresque fresque = getFresqueOrThrow(id);

        fresque.setTitre(request.getTitre());
        fresque.setImageUrl(request.getImageUrl());
        fresque.setIntroduction(request.getIntroduction());
        fresque.setConte(request.getConteId() != null
                ? entityManager.getReference(ConteDanse.class, request.getConteId())
                : null);

        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    public void supprimerFresque(UUID id) {
        fresqueRepository.delete(getFresqueOrThrow(id));
    }

    // ────────────────────────────────────────────────────────────────
    // HOTSPOTS — GAL-08/09/10/12/13
    // ────────────────────────────────────────────────────────────────

    public FresqueResponse ajouterHotspot(UUID fresqueId, HotspotRequest request) {
        Fresque fresque = getFresqueOrThrow(fresqueId);

        Hotspot hotspot = Hotspot.builder()
                .fresque(fresque)
                .titre(request.getTitre())
                .explicationDecouverte(request.getExplicationDecouverte())
                .explicationApprofondie(request.getExplicationApprofondie())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .ordreAffichage(request.getOrdreAffichage() != null ? request.getOrdreAffichage() : fresque.getHotspots().size())
                .actif(request.getActif() == null || request.getActif())
                .build();

        fresque.getHotspots().add(hotspot);
        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    public FresqueResponse modifierHotspot(UUID fresqueId, UUID hotspotId, HotspotRequest request) {
        Fresque fresque = getFresqueOrThrow(fresqueId);
        Hotspot hotspot = fresque.getHotspots().stream()
                .filter(h -> h.getId().equals(hotspotId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Hotspot non trouve avec l'ID: " + hotspotId));

        hotspot.setTitre(request.getTitre());
        hotspot.setExplicationDecouverte(request.getExplicationDecouverte());
        hotspot.setExplicationApprofondie(request.getExplicationApprofondie());
        hotspot.setImageUrl(request.getImageUrl());
        hotspot.setAudioUrl(request.getAudioUrl());
        if (request.getOrdreAffichage() != null) {
            hotspot.setOrdreAffichage(request.getOrdreAffichage());
        }
        if (request.getActif() != null) {
            hotspot.setActif(request.getActif());
        }

        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    public FresqueResponse supprimerHotspot(UUID fresqueId, UUID hotspotId) {
        Fresque fresque = getFresqueOrThrow(fresqueId);
        boolean supprime = fresque.getHotspots().removeIf(h -> h.getId().equals(hotspotId));
        if (!supprime) {
            throw new ResourceNotFoundException("Hotspot non trouve avec l'ID: " + hotspotId);
        }
        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private SalleGalerie getSalleOrThrow(UUID id) {
        return salleGalerieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle de galerie non trouvee avec l'ID: " + id));
    }

    private Fresque getFresqueOrThrow(UUID id) {
        return fresqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fresque non trouvee avec l'ID: " + id));
    }

    private SalleGalerieResponse toSalleResponse(SalleGalerie salle) {
        FresqueResponse fresque = fresqueRepository.findBySalleId(salle.getId())
                .map(this::toFresqueResponse)
                .orElse(null);

        return SalleGalerieResponse.builder()
                .id(salle.getId())
                .pays(salle.getPays())
                .introduction(salle.getIntroduction())
                .ambianceSonoreUrl(salle.getAmbianceSonoreUrl())
                .paletteCouleurs(salle.getPaletteCouleurs())
                .ordreAffichage(salle.getOrdreAffichage())
                .actif(salle.isActif())
                .fresque(fresque)
                .build();
    }

    private FresqueResponse toFresqueResponse(Fresque fresque) {
        List<HotspotResponse> hotspots = fresque.getHotspots().stream()
                .map(h -> HotspotResponse.builder()
                        .id(h.getId())
                        .fresqueId(fresque.getId())
                        .titre(h.getTitre())
                        .explicationDecouverte(h.getExplicationDecouverte())
                        .explicationApprofondie(h.getExplicationApprofondie())
                        .imageUrl(h.getImageUrl())
                        .audioUrl(h.getAudioUrl())
                        .ordreAffichage(h.getOrdreAffichage())
                        .actif(h.isActif())
                        .build())
                .collect(Collectors.toList());

        return FresqueResponse.builder()
                .id(fresque.getId())
                .salleId(fresque.getSalle().getId())
                .titre(fresque.getTitre())
                .imageUrl(fresque.getImageUrl())
                .introduction(fresque.getIntroduction())
                .conteId(fresque.getConte() != null ? fresque.getConte().getId() : null)
                .hotspots(hotspots)
                .build();
    }
}
