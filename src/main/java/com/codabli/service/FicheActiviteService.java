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
 * </p>
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
                .titre(request.titre())
                .objectif(request.objectif())
                .ageMin(request.ageMin())
                .ageMax(request.ageMax())
                .dureeMinutes(request.dureeMinutes())
                .materiel(request.materiel())
                .consignes(request.consignes())
                .deroulement(request.deroulement())
                .competences(request.competences())
                .adaptations(request.adaptations())
                .credits(request.credits())
                .fichierPdfUrl(request.fichierPdfUrl())
                .actif(request.actif() == null || request.actif())
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

        fiche.setTitre(request.titre());
        fiche.setObjectif(request.objectif());
        fiche.setAgeMin(request.ageMin());
        fiche.setAgeMax(request.ageMax());
        fiche.setDureeMinutes(request.dureeMinutes());
        fiche.setMateriel(request.materiel());
        fiche.setConsignes(request.consignes());
        fiche.setDeroulement(request.deroulement());
        fiche.setCompetences(request.competences());
        fiche.setAdaptations(request.adaptations());
        fiche.setCredits(request.credits());
        fiche.setFichierPdfUrl(request.fichierPdfUrl());
        if (request.actif() != null) {
            fiche.setActif(request.actif());
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
        return new FicheActiviteResponse(
                fiche.getId(),
                fiche.getTitre(),
                fiche.getObjectif(),
                fiche.getAgeMin(),
                fiche.getAgeMax(),
                fiche.getDureeMinutes(),
                fiche.getMateriel(),
                fiche.getConsignes(),
                fiche.getDeroulement(),
                fiche.getCompetences(),
                fiche.getAdaptations(),
                fiche.getCredits(),
                fiche.getFichierPdfUrl(),
                fiche.isActif(),
                fiche.getDateCreation(),
                fiche.getDateMiseAJour());
    }
}
