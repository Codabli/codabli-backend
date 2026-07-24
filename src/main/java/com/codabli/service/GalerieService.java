package com.codabli.service;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.dto.GalerieMiseEnAvantRequest;
import com.codabli.dto.GalerieMiseEnAvantResponse;
import com.codabli.entity.CarteAConte;
import com.codabli.entity.GalerieMiseEnAvant;
import com.codabli.entity.enums.StatutModeration;
import com.codabli.repository.CarteAConteRepository;
import com.codabli.repository.GalerieMiseEnAvantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la galerie d'art.
 *
 * Sécurité :
 * - Lecture (GET galerie) : publique, filtrée sur statutModeration = valide
 * - Gestion mises en avant : réservée aux admins via SecurityConfig
 * (/api/admin/**)
 *
 * Règle clé : une carte mise en avant dont le statut redevient non-valide
 * disparaît automatiquement de la galerie grâce au WHERE JPQL.
 */
@Service
@Transactional
public class GalerieService {

    private final GalerieMiseEnAvantRepository galerieMiseEnAvantRepository;
    private final CarteAConteRepository carteAConteRepository;

    public GalerieService(GalerieMiseEnAvantRepository galerieMiseEnAvantRepository,
            CarteAConteRepository carteAConteRepository) {
        this.galerieMiseEnAvantRepository = galerieMiseEnAvantRepository;
        this.carteAConteRepository = carteAConteRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER GALERIE — public
    // ────────────────────────────────────────────────────────────────

    /**
     * Retourne la liste paginée des cartes validées pour la galerie publique.
     * Les mises en avant actives apparaissent en premier.
     */
    @Transactional(readOnly = true)
    public Page<GalerieItemResponse> listerGalerie(Pageable pageable) {
        return galerieMiseEnAvantRepository.findCartesForGalerie(
                StatutModeration.valide,
                OffsetDateTime.now(),
                pageable);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL CARTE — public
    // ────────────────────────────────────────────────────────────────

    /**
     * Retourne le détail d'une carte validée.
     * Si la carte n'existe pas ou n'est pas validée, lance
     * ResourceNotFoundException (404).
     */
    @Transactional(readOnly = true)
    public GalerieItemResponse getCarteGalerie(UUID id) {
        CarteAConte carte = carteAConteRepository
                .findByIdAndStatutModeration(id, StatutModeration.valide)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carte non trouvée ou non validée avec l'ID: " + id));

        return GalerieItemResponse.builder()
                .id(carte.getId())
                .type(carte.getType())
                .imageUrl(carte.getImageUrl())
                .texteAssocie(carte.getTexteAssocie())
                .dateCreation(carte.getDateCreation())
                .createurPrenom(carte.getCreateur().getPrenom())
                .miseEnAvant(false)
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // AJOUTER MISE EN AVANT — admin
    // ────────────────────────────────────────────────────────────────

    /**
     * Ajoute une mise en avant pour une carte validée.
     * Vérifie que la carte existe et est validée avant l'ajout.
     */
    public GalerieMiseEnAvantResponse ajouterMiseEnAvant(GalerieMiseEnAvantRequest request) {
        CarteAConte carte = carteAConteRepository
                .findByIdAndStatutModeration(request.getCarteAConteId(), StatutModeration.valide)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carte non trouvée ou non validée avec l'ID: " + request.getCarteAConteId()));

        GalerieMiseEnAvant miseEnAvant = GalerieMiseEnAvant.builder()
                .carteAConte(carte)
                .dateFin(request.getDateFin())
                .ordreAffichage(request.getOrdreAffichage())
                .actif(true)
                .build();

        GalerieMiseEnAvant saved = galerieMiseEnAvantRepository.save(miseEnAvant);
        return toMiseEnAvantResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER MISE EN AVANT — admin
    // ────────────────────────────────────────────────────────────────

    /**
     * Supprime une entrée de mise en avant.
     */
    public void supprimerMiseEnAvant(UUID id) {
        GalerieMiseEnAvant miseEnAvant = galerieMiseEnAvantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise en avant non trouvée avec l'ID: " + id));
        galerieMiseEnAvantRepository.delete(miseEnAvant);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER MISES EN AVANT — admin
    // ────────────────────────────────────────────────────────────────

    /**
     * Liste toutes les mises en avant (pour l'interface admin).
     */
    @Transactional(readOnly = true)
    public List<GalerieMiseEnAvantResponse> listerMisesEnAvant() {
        return galerieMiseEnAvantRepository.findAll().stream()
                .map(this::toMiseEnAvantResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private GalerieMiseEnAvantResponse toMiseEnAvantResponse(GalerieMiseEnAvant m) {
        String titreCarte = m.getCarteAConte().getTexteAssocie() != null
                ? m.getCarteAConte().getTexteAssocie()
                : m.getCarteAConte().getType().name();

        return GalerieMiseEnAvantResponse.builder()
                .id(m.getId())
                .carteAConteId(m.getCarteAConte().getId())
                .titreCarte(titreCarte)
                .dateDebut(m.getDateDebut())
                .dateFin(m.getDateFin())
                .ordreAffichage(m.getOrdreAffichage())
                .actif(m.isActif())
                .build();
    }
}
