package com.codabli.service;

import com.codabli.dto.ElementCarnetVoyageRequest;
import com.codabli.dto.ElementCarnetVoyageResponse;
import com.codabli.dto.PageCarnetVoyageRequest;
import com.codabli.dto.PageCarnetVoyageResponse;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ElementCarnetVoyage;
import com.codabli.entity.PageCarnetVoyage;
import com.codabli.entity.ProfilEnfant;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutPageCarnet;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.PageCarnetVoyageRepository;
import com.codabli.repository.ProfilEnfantRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour le Carnet de Voyage (CDC 8.17.2, CDV-01 a 14).
 * Meme logique de securite et de non-generation de contenu que
 * {@link CarnetLectureService} (RG-10, RG-13).
 */
@Service
@Transactional
public class CarnetVoyageService {

    private final PageCarnetVoyageRepository pageRepository;
    private final ProfilEnfantRepository profilEnfantRepository;
    private final ConteDanseRepository conteDanseRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CarnetVoyageService(PageCarnetVoyageRepository pageRepository,
            ProfilEnfantRepository profilEnfantRepository,
            ConteDanseRepository conteDanseRepository,
            UtilisateurRepository utilisateurRepository) {
        this.pageRepository = pageRepository;
        this.profilEnfantRepository = profilEnfantRepository;
        this.conteDanseRepository = conteDanseRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER UNE PAGE — CDV-01
    // ────────────────────────────────────────────────────────────────

    public PageCarnetVoyageResponse creerPage(UUID profilEnfantId, PageCarnetVoyageRequest request, Jwt jwt) {
        ProfilEnfant profil = getProfilAvecDroit(profilEnfantId, jwt);

        PageCarnetVoyage page = PageCarnetVoyage.builder()
                .profilEnfant(profil)
                .pays(request.getPays())
                .drapeauUrl(request.getDrapeauUrl())
                .languesDecouvertes(request.getLanguesDecouvertes())
                .dateVisite(request.getDateVisite() != null ? request.getDateVisite() : LocalDate.now())
                .identiteNotes(request.getIdentiteNotes())
                .natureNotes(request.getNatureNotes())
                .societeNotes(request.getSocieteNotes())
                .cultureNotes(request.getCultureNotes())
                .experienceNotes(request.getExperienceNotes())
                .build();

        if (request.getConteId() != null) {
            ConteDanse conte = conteDanseRepository.findById(request.getConteId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conte danse non trouve avec l'ID: " + request.getConteId()));
            page.setConte(conte);
            if (page.getPays() == null) {
                page.setPays(conte.getPays());
            }
            if (page.getLanguesDecouvertes() == null) {
                page.setLanguesDecouvertes(conte.getLangueOriginale());
            }
        }

        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // CONSULTER LE CARNET — CDV-14
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PageCarnetVoyageResponse> listerPages(UUID profilEnfantId, Jwt jwt) {
        getProfilAvecDroit(profilEnfantId, jwt);
        return pageRepository.findByProfilEnfantIdOrderByDateCreationDesc(profilEnfantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // PREVISUALISER UNE PAGE — CDV-11
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageCarnetVoyageResponse getPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        return toResponse(getPageAvecDroit(profilEnfantId, pageId, jwt));
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER UNE PAGE
    // ────────────────────────────────────────────────────────────────

    public PageCarnetVoyageResponse modifierPage(UUID profilEnfantId, UUID pageId,
            PageCarnetVoyageRequest request, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        if (page.getStatut() == StatutPageCarnet.terminee || page.getStatut() == StatutPageCarnet.non_commencee) {
            page.setStatut(StatutPageCarnet.en_cours);
        }

        page.setPays(request.getPays());
        page.setDrapeauUrl(request.getDrapeauUrl());
        page.setLanguesDecouvertes(request.getLanguesDecouvertes());
        page.setDateVisite(request.getDateVisite());
        page.setIdentiteNotes(request.getIdentiteNotes());
        page.setNatureNotes(request.getNatureNotes());
        page.setSocieteNotes(request.getSocieteNotes());
        page.setCultureNotes(request.getCultureNotes());
        page.setExperienceNotes(request.getExperienceNotes());

        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // TERMINER UNE PAGE — CDV-12
    // ────────────────────────────────────────────────────────────────

    public PageCarnetVoyageResponse terminerPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);
        page.setStatut(StatutPageCarnet.terminee);
        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER UNE PAGE
    // ────────────────────────────────────────────────────────────────

    public void supprimerPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);
        pageRepository.delete(page);
    }

    // ────────────────────────────────────────────────────────────────
    // ELEMENTS (nature/culture/personnages/objets/creations) — CDV-04/06/09/10
    // ────────────────────────────────────────────────────────────────

    public PageCarnetVoyageResponse ajouterElement(UUID profilEnfantId, UUID pageId,
            ElementCarnetVoyageRequest request, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        ElementCarnetVoyage element = ElementCarnetVoyage.builder()
                .page(page)
                .type(request.getType())
                .nom(request.getNom())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .ordreAffichage(request.getOrdreAffichage() != null ? request.getOrdreAffichage() : page.getElements().size())
                .build();

        page.getElements().add(element);
        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    public PageCarnetVoyageResponse supprimerElement(UUID profilEnfantId, UUID pageId, UUID elementId, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        boolean supprime = page.getElements().removeIf(e -> e.getId().equals(elementId));
        if (!supprime) {
            throw new ResourceNotFoundException("Element non trouve avec l'ID: " + elementId);
        }

        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private ProfilEnfant getProfilAvecDroit(UUID profilEnfantId, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        ProfilEnfant profil = profilEnfantRepository.findById(profilEnfantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profil enfant non trouve avec l'ID: " + profilEnfantId));

        boolean estResponsable = profil.getResponsable().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        if (!estResponsable && !estAdmin) {
            throw new AccessDeniedException("Ce carnet ne vous appartient pas");
        }

        return profil;
    }

    private PageCarnetVoyage getPageAvecDroit(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        getProfilAvecDroit(profilEnfantId, jwt);

        PageCarnetVoyage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Page de carnet de voyage non trouvee avec l'ID: " + pageId));

        if (!page.getProfilEnfant().getId().equals(profilEnfantId)) {
            throw new ResourceNotFoundException(
                    "Page de carnet de voyage non trouvee avec l'ID: " + pageId);
        }

        return page;
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

    private PageCarnetVoyageResponse toResponse(PageCarnetVoyage page) {
        List<ElementCarnetVoyageResponse> elements = page.getElements().stream()
                .map(e -> ElementCarnetVoyageResponse.builder()
                        .id(e.getId())
                        .type(e.getType())
                        .nom(e.getNom())
                        .description(e.getDescription())
                        .imageUrl(e.getImageUrl())
                        .ordreAffichage(e.getOrdreAffichage())
                        .build())
                .collect(Collectors.toList());

        return PageCarnetVoyageResponse.builder()
                .id(page.getId())
                .profilEnfantId(page.getProfilEnfant().getId())
                .conteId(page.getConte() != null ? page.getConte().getId() : null)
                .pays(page.getPays())
                .drapeauUrl(page.getDrapeauUrl())
                .languesDecouvertes(page.getLanguesDecouvertes())
                .dateVisite(page.getDateVisite())
                .identiteNotes(page.getIdentiteNotes())
                .natureNotes(page.getNatureNotes())
                .societeNotes(page.getSocieteNotes())
                .cultureNotes(page.getCultureNotes())
                .experienceNotes(page.getExperienceNotes())
                .statut(page.getStatut())
                .fichierExportUrl(page.getFichierExportUrl())
                .elements(elements)
                .dateCreation(page.getDateCreation())
                .dateMiseAJour(page.getDateMiseAJour())
                .build();
    }
}
