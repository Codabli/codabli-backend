package com.codabli.service;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des ressources de la mallette pédagogique.
 */
@Service
@Transactional
public class RessourcePedagogiqueService {

    private final RessourcePedagogiqueRepository ressourcePedagogiqueRepository;

    public RessourcePedagogiqueService(RessourcePedagogiqueRepository ressourcePedagogiqueRepository) {
        this.ressourcePedagogiqueRepository = ressourcePedagogiqueRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RessourcePedagogiqueResponse> lister(
            TypeRessource type,
            String thematique,
            String niveauScolaire,
            Boolean actif,
            Jwt jwt,
            Pageable pageable) {

        Boolean actifFilter = actif;
        if (!hasWritePermission(jwt)) {
            // Enseignant/Professionnel de l'éducation : ne voient que l'actif
            actifFilter = true;
        }

        return ressourcePedagogiqueRepository.findByFilters(type, thematique, niveauScolaire, actifFilter, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // COCHAGE ID / VISIBILITE
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RessourcePedagogiqueResponse getById(UUID id, Jwt jwt) {
        RessourcePedagogique ressource = ressourcePedagogiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ressource pedagogique non trouvee avec l'ID: " + id));

        // Vérification explicite du flag actif
        if (!ressource.isActif() && !hasWritePermission(jwt)) {
            throw new AccessDeniedException("Vous n'êtes pas autorise a acceder a cette ressource inactive.");
        }

        return toResponse(ressource);
    }

    // ────────────────────────────────────────────────────────────────
    // CREER (admin & comite_lecture)
    // ────────────────────────────────────────────────────────────────

    public RessourcePedagogiqueResponse creer(RessourcePedagogiqueRequest request) {
        RessourcePedagogique ressource = RessourcePedagogique.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .type(request.getType())
                .fichierUrl(request.getFichierUrl())
                .thematique(request.getThematique())
                .niveauScolaire(request.getNiveauScolaire())
                .actif(request.getActif() != null ? request.getActif() : true)
                .build();

        RessourcePedagogique saved = ressourcePedagogiqueRepository.save(ressource);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER (admin & comite_lecture)
    // ────────────────────────────────────────────────────────────────

    public RessourcePedagogiqueResponse modifier(UUID id, RessourcePedagogiqueRequest request) {
        RessourcePedagogique ressource = ressourcePedagogiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ressource pedagogique non trouvee avec l'ID: " + id));

        ressource.setTitre(request.getTitre());
        ressource.setDescription(request.getDescription());
        ressource.setType(request.getType());
        ressource.setFichierUrl(request.getFichierUrl());
        ressource.setThematique(request.getThematique());
        ressource.setNiveauScolaire(request.getNiveauScolaire());
        if (request.getActif() != null) {
            ressource.setActif(request.getActif());
        }

        RessourcePedagogique saved = ressourcePedagogiqueRepository.save(ressource);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER (admin uniquement)
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new AccessDeniedException("Seul un administrateur peut supprimer definitivement une ressource");
        }

        RessourcePedagogique ressource = ressourcePedagogiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ressource pedagogique non trouvee avec l'ID: " + id));

        ressourcePedagogiqueRepository.delete(ressource);
    }

    // ────────────────────────────────────────────────────────────────
    // SECURITE INTERNE
    // ────────────────────────────────────────────────────────────────

    private boolean hasWritePermission(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsMap("realm_access") == null) {
            return false;
        }
        Object rolesObj = jwt.getClaimAsMap("realm_access").get("roles");
        if (rolesObj instanceof List<?> roles) {
            return roles.contains("admin") || roles.contains("comite_lecture");
        }
        return false;
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsMap("realm_access") == null) {
            return false;
        }
        Object rolesObj = jwt.getClaimAsMap("realm_access").get("roles");
        if (rolesObj instanceof List<?> roles) {
            return roles.contains("admin");
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────
    // MAPPAGE
    // ────────────────────────────────────────────────────────────────

    private RessourcePedagogiqueResponse toResponse(RessourcePedagogique ressource) {
        return RessourcePedagogiqueResponse.builder()
                .id(ressource.getId())
                .titre(ressource.getTitre())
                .description(ressource.getDescription())
                .type(ressource.getType())
                .fichierUrl(ressource.getFichierUrl())
                .thematique(ressource.getThematique())
                .niveauScolaire(ressource.getNiveauScolaire())
                .dateAjout(ressource.getDateAjout())
                .actif(ressource.isActif())
                .build();
    }
}
