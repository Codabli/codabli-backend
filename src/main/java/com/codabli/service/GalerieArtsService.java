package com.codabli.service;

import com.codabli.dto.*;
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

/**
 * Service metier pour le socle structurel de la Galerie des Arts immersive
 * (CDC 8.11) : hall (salles/pays), fresques principales et hotspots.
 * <p>
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
                .toList();
    }

    @Transactional(readOnly = true)
    public SalleGalerieResponse getSalle(UUID id) {
        return toSalleResponse(getSalleOrThrow(id));
    }

    public SalleGalerieResponse creerSalle(SalleGalerieRequest request) {
        SalleGalerie salle = SalleGalerie.builder()
                .pays(request.pays())
                .introduction(request.introduction())
                .ambianceSonoreUrl(request.ambianceSonoreUrl())
                .paletteCouleurs(request.paletteCouleurs())
                .ordreAffichage(request.ordreAffichage() != null ? request.ordreAffichage() : 0)
                .actif(request.actif() == null || request.actif())
                .build();
        return toSalleResponse(salleGalerieRepository.save(salle));
    }

    public SalleGalerieResponse modifierSalle(UUID id, SalleGalerieRequest request) {
        SalleGalerie salle = getSalleOrThrow(id);
        salle.setPays(request.pays());
        salle.setIntroduction(request.introduction());
        salle.setAmbianceSonoreUrl(request.ambianceSonoreUrl());
        salle.setPaletteCouleurs(request.paletteCouleurs());
        if (request.ordreAffichage() != null) {
            salle.setOrdreAffichage(request.ordreAffichage());
        }
        if (request.actif() != null) {
            salle.setActif(request.actif());
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
        SalleGalerie salle = getSalleOrThrow(request.salleId());

        if (fresqueRepository.findBySalleId(salle.getId()).isPresent()) {
            throw new IllegalStateException("Cette salle possede deja une fresque principale");
        }

        Fresque fresque = Fresque.builder()
                .salle(salle)
                .titre(request.titre())
                .imageUrl(request.imageUrl())
                .introduction(request.introduction())
                .build();

        if (request.conteId() != null) {
            fresque.setConte(entityManager.getReference(ConteDanse.class, request.conteId()));
        }

        return toFresqueResponse(fresqueRepository.save(fresque));
    }

    public FresqueResponse modifierFresque(UUID id, FresqueRequest request) {
        Fresque fresque = getFresqueOrThrow(id);

        fresque.setTitre(request.titre());
        fresque.setImageUrl(request.imageUrl());
        fresque.setIntroduction(request.introduction());
        fresque.setConte(request.conteId() != null
                ? entityManager.getReference(ConteDanse.class, request.conteId())
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
                .titre(request.titre())
                .explicationDecouverte(request.explicationDecouverte())
                .explicationApprofondie(request.explicationApprofondie())
                .imageUrl(request.imageUrl())
                .audioUrl(request.audioUrl())
                .ordreAffichage(request.ordreAffichage() != null ? request.ordreAffichage() : fresque.getHotspots().size())
                .actif(request.actif() == null || request.actif())
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

        hotspot.setTitre(request.titre());
        hotspot.setExplicationDecouverte(request.explicationDecouverte());
        hotspot.setExplicationApprofondie(request.explicationApprofondie());
        hotspot.setImageUrl(request.imageUrl());
        hotspot.setAudioUrl(request.audioUrl());
        if (request.ordreAffichage() != null) {
            hotspot.setOrdreAffichage(request.ordreAffichage());
        }
        if (request.actif() != null) {
            hotspot.setActif(request.actif());
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

        return new SalleGalerieResponse(
                salle.getId(),
                salle.getPays(),
                salle.getIntroduction(),
                salle.getAmbianceSonoreUrl(),
                salle.getPaletteCouleurs(),
                salle.getOrdreAffichage(),
                salle.isActif(),
                fresque);
    }

    private FresqueResponse toFresqueResponse(Fresque fresque) {
        List<HotspotResponse> hotspots = fresque.getHotspots().stream()
                .map(h -> new HotspotResponse(
                        h.getId(),
                        fresque.getId(),
                        h.getTitre(),
                        h.getExplicationDecouverte(),
                        h.getExplicationApprofondie(),
                        h.getImageUrl(),
                        h.getAudioUrl(),
                        h.getOrdreAffichage(),
                        h.isActif()))
                .toList();

        return new FresqueResponse(
                fresque.getId(),
                fresque.getSalle().getId(),
                fresque.getTitre(),
                fresque.getImageUrl(),
                fresque.getIntroduction(),
                fresque.getConte() != null ? fresque.getConte().getId() : null,
                hotspots);
    }
}
