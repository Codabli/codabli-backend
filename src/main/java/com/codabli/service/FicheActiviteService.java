package com.codabli.service;

import com.codabli.dto.FicheActiviteRequest;
import com.codabli.dto.FicheActiviteResponse;
import com.codabli.entity.FicheActivite;
import com.codabli.repository.FicheActiviteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

/**
 * Service metier pour les fiches d'activites (CDC 8.7, FIC-ACT-01/02).
 *
 * Securite, identique a la Mallette Pedagogique :
 * - GET : enseignant/professionnel_education/admin/super_admin/comite_lecture,
 * filtre sur actif=true pour les non-gestionnaires
 * - POST/PUT : admin/super_admin/comite_lecture
 * - DELETE : admin/super_admin uniquement
 */
@Service
@Transactional
public class FicheActiviteService {

    private final FicheActiviteRepository ficheActiviteRepository;

    public FicheActiviteService(FicheActiviteRepository ficheActiviteRepository) {
        this.ficheActiviteRepository = ficheActiviteRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<FicheActiviteResponse> lister(Integer age, Jwt jwt, Pageable pageable) {
        Boolean actifFiltre = hasWritePermission(jwt) ? null : Boolean.TRUE;
        return ficheActiviteRepository.findByFilters(actifFiltre, age, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FicheActiviteResponse getById(UUID id, Jwt jwt) {
        FicheActivite fiche = ficheActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fiche d'activite non trouvee avec l'ID: " + id));

        if (!fiche.isActif() && !hasWritePermission(jwt)) {
            throw new AccessDeniedException("Vous n'etes pas autorise a acceder a cette fiche inactive");
        }

        return toResponse(fiche);
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — admin/super_admin/comite_lecture
    // ────────────────────────────────────────────────────────────────

    public FicheActiviteResponse creer(FicheActiviteRequest request) {
        FicheActivite fiche = FicheActivite.builder()
                .titre(request.getTitre())
                .objectif(request.getObjectif())
                .ageMin(request.getAgeMin())
                .ageMax(request.getAgeMax())
                .dureeMinutes(request.getDureeMinutes())
                .materiel(request.getMateriel())
                .consignes(request.getConsignes())
                .deroulement(request.getDeroulement())
                .competences(request.getCompetences())
                .adaptations(request.getAdaptations())
                .credits(request.getCredits())
                .fichierPdfUrl(request.getFichierPdfUrl())
                .actif(request.getActif() == null || request.getActif())
                .build();

        FicheActivite saved = ficheActiviteRepository.save(fiche);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — admin/super_admin/comite_lecture
    // ────────────────────────────────────────────────────────────────

    public FicheActiviteResponse modifier(UUID id, FicheActiviteRequest request) {
        FicheActivite fiche = ficheActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fiche d'activite non trouvee avec l'ID: " + id));

        fiche.setTitre(request.getTitre());
        fiche.setObjectif(request.getObjectif());
        fiche.setAgeMin(request.getAgeMin());
        fiche.setAgeMax(request.getAgeMax());
        fiche.setDureeMinutes(request.getDureeMinutes());
        fiche.setMateriel(request.getMateriel());
        fiche.setConsignes(request.getConsignes());
        fiche.setDeroulement(request.getDeroulement());
        fiche.setCompetences(request.getCompetences());
        fiche.setAdaptations(request.getAdaptations());
        fiche.setCredits(request.getCredits());
        fiche.setFichierPdfUrl(request.getFichierPdfUrl());
        if (request.getActif() != null) {
            fiche.setActif(request.getActif());
        }

        FicheActivite saved = ficheActiviteRepository.save(fiche);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — admin/super_admin uniquement
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new AccessDeniedException("Seul un administrateur peut supprimer definitivement une fiche");
        }

        FicheActivite fiche = ficheActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fiche d'activite non trouvee avec l'ID: " + id));

        ficheActiviteRepository.delete(fiche);
    }

    // ────────────────────────────────────────────────────────────────
    // SECURITE INTERNE
    // ────────────────────────────────────────────────────────────────

    private boolean hasWritePermission(Jwt jwt) {
        Collection<String> roles = extractRoles(jwt);
        return roles.contains("admin") || roles.contains("super_admin") || roles.contains("comite_lecture");
    }

    private boolean isAdmin(Jwt jwt) {
        Collection<String> roles = extractRoles(jwt);
        return roles.contains("admin") || roles.contains("super_admin");
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsMap("realm_access") == null) {
            return java.util.Collections.emptyList();
        }
        Object rolesObj = jwt.getClaimAsMap("realm_access").get("roles");
        return rolesObj instanceof Collection<?> roles
                ? (Collection<String>) roles
                : java.util.Collections.emptyList();
    }

    // ────────────────────────────────────────────────────────────────
    // MAPPAGE
    // ────────────────────────────────────────────────────────────────

    private FicheActiviteResponse toResponse(FicheActivite fiche) {
        return FicheActiviteResponse.builder()
                .id(fiche.getId())
                .titre(fiche.getTitre())
                .objectif(fiche.getObjectif())
                .ageMin(fiche.getAgeMin())
                .ageMax(fiche.getAgeMax())
                .dureeMinutes(fiche.getDureeMinutes())
                .materiel(fiche.getMateriel())
                .consignes(fiche.getConsignes())
                .deroulement(fiche.getDeroulement())
                .competences(fiche.getCompetences())
                .adaptations(fiche.getAdaptations())
                .credits(fiche.getCredits())
                .fichierPdfUrl(fiche.getFichierPdfUrl())
                .actif(fiche.isActif())
                .dateCreation(fiche.getDateCreation())
                .dateMiseAJour(fiche.getDateMiseAJour())
                .build();
    }
}
