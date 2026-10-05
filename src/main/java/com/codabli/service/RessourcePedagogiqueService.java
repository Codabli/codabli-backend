package com.codabli.service;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.RessourceFavorite;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.repository.RessourceFavoriteRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import com.codabli.repository.UtilisateurRepository;
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
    private final RessourceFavoriteRepository ressourceFavoriteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public RessourcePedagogiqueService(RessourcePedagogiqueRepository ressourcePedagogiqueRepository,
            RessourceFavoriteRepository ressourceFavoriteRepository,
            UtilisateurRepository utilisateurRepository) {
        this.ressourcePedagogiqueRepository = ressourcePedagogiqueRepository;
        this.ressourceFavoriteRepository = ressourceFavoriteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // FAVORIS — RES-04
    // ────────────────────────────────────────────────────────────────

    public void ajouterFavori(UUID ressourceId, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        if (ressourceFavoriteRepository.findByUtilisateurIdAndRessourceId(utilisateur.getId(), ressourceId).isPresent()) {
            return;
        }
        RessourcePedagogique ressource = ressourcePedagogiqueRepository.findById(ressourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ressource pedagogique non trouvee avec l'ID: " + ressourceId));

        RessourceFavorite favori = RessourceFavorite.builder()
                .utilisateur(utilisateur)
                .ressource(ressource)
                .build();
        ressourceFavoriteRepository.save(favori);
    }

    public void retirerFavori(UUID ressourceId, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        ressourceFavoriteRepository.findByUtilisateurIdAndRessourceId(utilisateur.getId(), ressourceId)
                .ifPresent(ressourceFavoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<RessourcePedagogiqueResponse> mesFavoris(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return ressourceFavoriteRepository.findByUtilisateurId(utilisateur.getId()).stream()
                .map(f -> toResponse(f.getRessource()))
                .toList();
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        return utilisateurRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + jwt.getSubject()));
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
                .titre(request.titre())
                .description(request.description())
                .type(request.type())
                .fichierUrl(request.fichierUrl())
                .thematique(request.thematique())
                .niveauScolaire(request.niveauScolaire())
                .actif(request.actif() != null ? request.actif() : true)
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

        ressource.setTitre(request.titre());
        ressource.setDescription(request.description());
        ressource.setType(request.type());
        ressource.setFichierUrl(request.fichierUrl());
        ressource.setThematique(request.thematique());
        ressource.setNiveauScolaire(request.niveauScolaire());
        if (request.actif() != null) {
            ressource.setActif(request.actif());
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
        return new RessourcePedagogiqueResponse(
                ressource.getId(),
                ressource.getTitre(),
                ressource.getDescription(),
                ressource.getType(),
                ressource.getFichierUrl(),
                ressource.getThematique(),
                ressource.getNiveauScolaire(),
                ressource.getDateAjout(),
                ressource.isActif());
    }
}
