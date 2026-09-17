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
 * </p>
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
                .nom(request.nom())
                .logoUrl(request.logoUrl())
                .presentation(request.presentation())
                .categorie(request.categorie())
                .territoire(request.territoire())
                .roleProjet(request.roleProjet())
                .videoUrl(request.videoUrl())
                .lien(request.lien())
                .periodeDebut(request.periodeDebut())
                .periodeFin(request.periodeFin())
                .actif(request.actif() == null || request.actif())
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

        partenaire.setNom(request.nom());
        partenaire.setLogoUrl(request.logoUrl());
        partenaire.setPresentation(request.presentation());
        partenaire.setCategorie(request.categorie());
        partenaire.setTerritoire(request.territoire());
        partenaire.setRoleProjet(request.roleProjet());
        partenaire.setVideoUrl(request.videoUrl());
        partenaire.setLien(request.lien());
        partenaire.setPeriodeDebut(request.periodeDebut());
        partenaire.setPeriodeFin(request.periodeFin());
        if (request.actif() != null) {
            partenaire.setActif(request.actif());
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
        return new PartenaireResponse(
                partenaire.getId(),
                partenaire.getNom(),
                partenaire.getLogoUrl(),
                partenaire.getPresentation(),
                partenaire.getCategorie(),
                partenaire.getTerritoire(),
                partenaire.getRoleProjet(),
                partenaire.getVideoUrl(),
                partenaire.getLien(),
                partenaire.getPeriodeDebut(),
                partenaire.getPeriodeFin(),
                partenaire.isActif(),
                partenaire.getDateCreation(),
                partenaire.getDateMiseAJour());
    }
}
