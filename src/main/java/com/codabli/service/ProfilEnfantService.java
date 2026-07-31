package com.codabli.service;

import com.codabli.dto.ProfilEnfantRequest;
import com.codabli.dto.ProfilEnfantResponse;
import com.codabli.entity.ProfilEnfant;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ProfilEnfantRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les profils enfants (CDC FAM-02, ENF-01).
 *
 * Securite a deux niveaux, identique aux autres modules :
 * 1. @PreAuthorize dans le controller → seuls parent/professionnel_education
 * peuvent creer/gerer des profils enfants
 * 2. ICI : un responsable ne peut consulter/modifier/supprimer que SES
 * propres profils enfants (sauf admin/super_admin)
 */
@Service
@Transactional
public class ProfilEnfantService {

    private final ProfilEnfantRepository profilEnfantRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ProfilEnfantService(ProfilEnfantRepository profilEnfantRepository,
            UtilisateurRepository utilisateurRepository) {
        this.profilEnfantRepository = profilEnfantRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — FAM-02
    // ────────────────────────────────────────────────────────────────

    public ProfilEnfantResponse creer(ProfilEnfantRequest request, Jwt jwt) {
        Utilisateur responsable = getUtilisateurFromJwt(jwt);

        ProfilEnfant profil = ProfilEnfant.builder()
                .responsable(responsable)
                .pseudonyme(request.getPseudonyme())
                .dateNaissance(request.getDateNaissance())
                .languePreferee(request.getLanguePreferee())
                .preferences(request.getPreferences())
                .accessibilite(request.getAccessibilite())
                .build();

        appliquerAutorisation(profil, request.getAutorisationParentale());

        ProfilEnfant saved = profilEnfantRepository.save(profil);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER MES PROFILS — ENF-01
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProfilEnfantResponse> mesProfils(Jwt jwt) {
        Utilisateur responsable = getUtilisateurFromJwt(jwt);
        return profilEnfantRepository.findByResponsableIdOrderByPseudonyme(responsable.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — verification fine de propriete
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProfilEnfantResponse getById(UUID id, Jwt jwt) {
        return toResponse(getProfilAvecDroit(id, jwt));
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER
    // ────────────────────────────────────────────────────────────────

    public ProfilEnfantResponse modifier(UUID id, ProfilEnfantRequest request, Jwt jwt) {
        ProfilEnfant profil = getProfilAvecDroit(id, jwt);

        profil.setPseudonyme(request.getPseudonyme());
        profil.setDateNaissance(request.getDateNaissance());
        profil.setLanguePreferee(request.getLanguePreferee());
        profil.setPreferences(request.getPreferences());
        profil.setAccessibilite(request.getAccessibilite());

        if (request.getAutorisationParentale() != null) {
            appliquerAutorisation(profil, request.getAutorisationParentale());
        }

        ProfilEnfant saved = profilEnfantRepository.save(profil);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        ProfilEnfant profil = getProfilAvecDroit(id, jwt);
        profilEnfantRepository.delete(profil);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    /**
     * RG-04 : l'autorisation parentale est datee des qu'elle passe a true, et
     * la date est effacee si elle est revoquee.
     */
    private void appliquerAutorisation(ProfilEnfant profil, Boolean nouvelleValeur) {
        boolean valeur = Boolean.TRUE.equals(nouvelleValeur);
        boolean changeADateter = valeur && !profil.isAutorisationParentale();
        profil.setAutorisationParentale(valeur);
        if (changeADateter) {
            profil.setDateAutorisation(OffsetDateTime.now());
        } else if (!valeur) {
            profil.setDateAutorisation(null);
        }
    }

    private ProfilEnfant getProfilAvecDroit(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        ProfilEnfant profil = profilEnfantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profil enfant non trouve avec l'ID: " + id));

        boolean estResponsable = profil.getResponsable().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        if (!estResponsable && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez gerer que vos propres profils enfants");
        }

        return profil;
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return java.util.Collections.emptyList();
        }
        return (Collection<String>) realmAccess.get("roles");
    }

    private ProfilEnfantResponse toResponse(ProfilEnfant profil) {
        return ProfilEnfantResponse.builder()
                .id(profil.getId())
                .responsableId(profil.getResponsable().getId())
                .pseudonyme(profil.getPseudonyme())
                .dateNaissance(profil.getDateNaissance())
                .languePreferee(profil.getLanguePreferee())
                .preferences(profil.getPreferences())
                .accessibilite(profil.getAccessibilite())
                .autorisationParentale(profil.isAutorisationParentale())
                .dateAutorisation(profil.getDateAutorisation())
                .actif(profil.isActif())
                .dateCreation(profil.getDateCreation())
                .build();
    }
}
