package com.codabli.service;

import com.codabli.dto.PartenaireRequest;
import com.codabli.dto.PartenaireResponse;
import com.codabli.entity.Partenaire;
import com.codabli.entity.enums.CategoriePartenaire;
import com.codabli.repository.PartenaireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service metier pour les partenaires (CDC 8.13, PAR-01/02).
 *
 * Securite :
 * - Lecture (GET) : publique, filtree sur actif=true
 * - Ecriture (POST/PUT/DELETE) : reservee aux admins via @PreAuthorize dans
 * le controller
 */
@Service
@Transactional
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;

    public PartenaireService(PartenaireRepository partenaireRepository) {
        this.partenaireRepository = partenaireRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les partenaires actifs
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PartenaireResponse> lister(CategoriePartenaire categorie, Pageable pageable) {
        Page<Partenaire> page = categorie != null
                ? partenaireRepository.findByActifTrueAndCategorie(categorie, pageable)
                : partenaireRepository.findByActifTrue(pageable);
        return page.map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // RECHERCHE — utilisee par RechercheController (REC-01)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PartenaireResponse> rechercher(String query, Pageable pageable) {
        return partenaireRepository.rechercherActifs(query, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PartenaireResponse getById(UUID id) {
        Partenaire partenaire = partenaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partenaire non trouve avec l'ID: " + id));
        return toResponse(partenaire);
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — admin only
    // ────────────────────────────────────────────────────────────────

    public PartenaireResponse creer(PartenaireRequest request) {
        Partenaire partenaire = Partenaire.builder()
                .nom(request.getNom())
                .logoUrl(request.getLogoUrl())
                .presentation(request.getPresentation())
                .categorie(request.getCategorie())
                .territoire(request.getTerritoire())
                .roleProjet(request.getRoleProjet())
                .videoUrl(request.getVideoUrl())
                .lien(request.getLien())
                .periodeDebut(request.getPeriodeDebut())
                .periodeFin(request.getPeriodeFin())
                .actif(request.getActif() == null || request.getActif())
                .build();

        Partenaire saved = partenaireRepository.save(partenaire);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — admin only
    // ────────────────────────────────────────────────────────────────

    public PartenaireResponse modifier(UUID id, PartenaireRequest request) {
        Partenaire partenaire = partenaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partenaire non trouve avec l'ID: " + id));

        partenaire.setNom(request.getNom());
        partenaire.setLogoUrl(request.getLogoUrl());
        partenaire.setPresentation(request.getPresentation());
        partenaire.setCategorie(request.getCategorie());
        partenaire.setTerritoire(request.getTerritoire());
        partenaire.setRoleProjet(request.getRoleProjet());
        partenaire.setVideoUrl(request.getVideoUrl());
        partenaire.setLien(request.getLien());
        partenaire.setPeriodeDebut(request.getPeriodeDebut());
        partenaire.setPeriodeFin(request.getPeriodeFin());
        if (request.getActif() != null) {
            partenaire.setActif(request.getActif());
        }

        Partenaire saved = partenaireRepository.save(partenaire);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — admin only
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id) {
        Partenaire partenaire = partenaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partenaire non trouve avec l'ID: " + id));
        partenaireRepository.delete(partenaire);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private PartenaireResponse toResponse(Partenaire partenaire) {
        return PartenaireResponse.builder()
                .id(partenaire.getId())
                .nom(partenaire.getNom())
                .logoUrl(partenaire.getLogoUrl())
                .presentation(partenaire.getPresentation())
                .categorie(partenaire.getCategorie())
                .territoire(partenaire.getTerritoire())
                .roleProjet(partenaire.getRoleProjet())
                .videoUrl(partenaire.getVideoUrl())
                .lien(partenaire.getLien())
                .periodeDebut(partenaire.getPeriodeDebut())
                .periodeFin(partenaire.getPeriodeFin())
                .actif(partenaire.isActif())
                .dateCreation(partenaire.getDateCreation())
                .dateMiseAJour(partenaire.getDateMiseAJour())
                .build();
    }
}
