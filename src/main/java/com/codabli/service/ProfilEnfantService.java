package com.codabli.service;

import com.codabli.dto.ProfilEnfantRequest;
import com.codabli.dto.ProfilEnfantResponse;
import com.codabli.entity.ProfilEnfant;
import com.codabli.entity.Utilisateur;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ProfilEnfantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les profils enfants (CDC FAM-02, ENF-01).
 * </p>
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
    private final UtilisateurService utilisateurService;

    public ProfilEnfantService(ProfilEnfantRepository profilEnfantRepository, UtilisateurService utilisateurService) {
        this.profilEnfantRepository = profilEnfantRepository;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — FAM-02
    // ────────────────────────────────────────────────────────────────

    public ProfilEnfantResponse creer(ProfilEnfantRequest request, Jwt jwt) {
        Utilisateur responsable = utilisateurService.getUtilisateurFromJwt(jwt);

        ProfilEnfant profil = ProfilEnfant.builder()
                .responsable(responsable)
                .pseudonyme(request.pseudonyme())
                .dateNaissance(request.dateNaissance())
                .languePreferee(request.languePreferee())
                .preferences(request.preferences())
                .accessibilite(request.accessibilite())
                .build();

        appliquerAutorisation(profil, request.autorisationParentale());

        ProfilEnfant saved = profilEnfantRepository.save(profil);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER MES PROFILS — ENF-01
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProfilEnfantResponse> mesProfils(Jwt jwt) {
        Utilisateur responsable = utilisateurService.getUtilisateurFromJwt(jwt);
        return profilEnfantRepository.findByResponsableIdOrderByPseudonyme(responsable.getId())
                .stream()
                .map(this::toResponse)
                .toList();
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

        profil.setPseudonyme(request.pseudonyme());
        profil.setDateNaissance(request.dateNaissance());
        profil.setLanguePreferee(request.languePreferee());
        profil.setPreferences(request.preferences());
        profil.setAccessibilite(request.accessibilite());

        if (request.autorisationParentale() != null) {
            appliquerAutorisation(profil, request.autorisationParentale());
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
    public ProfilEnfant getById(UUID id){
        return profilEnfantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_PROFIL_ENFANT,
                        "Profil enfant non trouve avec l'ID: " + id));
    }

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
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);

        ProfilEnfant profil = getById(id);

        boolean estResponsable = profil.getResponsable().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        if (!estResponsable && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez gerer que vos propres profils enfants");
        }

        return profil;
    }

    private ProfilEnfantResponse toResponse(ProfilEnfant profil) {
        return new ProfilEnfantResponse(
                profil.getId(),
                profil.getResponsable().getId(),
                profil.getPseudonyme(),
                profil.getDateNaissance(),
                profil.getLanguePreferee(),
                profil.getPreferences(),
                profil.getAccessibilite(),
                profil.isAutorisationParentale(),
                profil.getDateAutorisation(),
                profil.isActif(),
                profil.getDateCreation());
    }
}
