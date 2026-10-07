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
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.PageCarnetVoyageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service metier pour le Carnet de Voyage (CDC 8.17.2, CDV-01 a 14).
 * Meme logique de securite et de non-generation de contenu que
 * {@link CarnetLectureService} (RG-10, RG-13).
 */
@Service
@Transactional
public class CarnetVoyageService {

    private final PageCarnetVoyageRepository pageRepository;

    private final ConteDanseService conteDanseService;
    private final ProfilEnfantService profilEnfantService;
    private final UtilisateurService utilisateurService;

    public CarnetVoyageService(PageCarnetVoyageRepository pageRepository,
                               ConteDanseService conteDanseService,
                               ProfilEnfantService profilEnfantService,
                               UtilisateurService utilisateurService) {
        this.pageRepository = pageRepository;
        this.conteDanseService = conteDanseService;
        this.profilEnfantService = profilEnfantService;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER UNE PAGE — CDV-01
    // ────────────────────────────────────────────────────────────────

    public PageCarnetVoyageResponse creerPage(UUID profilEnfantId, PageCarnetVoyageRequest request, Jwt jwt) {
        ProfilEnfant profil = getProfilAvecDroit(profilEnfantId, jwt);

        PageCarnetVoyage page = PageCarnetVoyage.builder()
                .profilEnfant(profil)
                .pays(request.pays())
                .drapeauUrl(request.drapeauUrl())
                .languesDecouvertes(request.languesDecouvertes())
                .dateVisite(request.dateVisite() != null ? request.dateVisite() : LocalDate.now())
                .identiteNotes(request.identiteNotes())
                .natureNotes(request.natureNotes())
                .societeNotes(request.societeNotes())
                .cultureNotes(request.cultureNotes())
                .experienceNotes(request.experienceNotes())
                .build();

        if (request.conteId() != null) {
            ConteDanse conte = conteDanseService.findById(request.conteId());
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
                .toList();
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

        page.setPays(request.pays());
        page.setDrapeauUrl(request.drapeauUrl());
        page.setLanguesDecouvertes(request.languesDecouvertes());
        page.setDateVisite(request.dateVisite());
        page.setIdentiteNotes(request.identiteNotes());
        page.setNatureNotes(request.natureNotes());
        page.setSocieteNotes(request.societeNotes());
        page.setCultureNotes(request.cultureNotes());
        page.setExperienceNotes(request.experienceNotes());

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
                .type(request.type())
                .nom(request.nom())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .ordreAffichage(request.ordreAffichage() != null ? request.ordreAffichage() : page.getElements().size())
                .build();

        page.getElements().add(element);
        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    public PageCarnetVoyageResponse supprimerElement(UUID profilEnfantId, UUID pageId, UUID elementId, Jwt jwt) {
        PageCarnetVoyage page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        boolean supprime = page.getElements().removeIf(e -> e.getId().equals(elementId));
        if (!supprime) {
            throw new ResourceNotFoundException(ErrorCode.UNKNOWN_CARNET_ELEMENT, "Element non trouve avec l'ID: " + elementId);
        }

        PageCarnetVoyage saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private ProfilEnfant getProfilAvecDroit(UUID profilEnfantId, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService. extractRoles(jwt);

        ProfilEnfant profil = profilEnfantService.getById(profilEnfantId);

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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_CARNET_VOYAGE,
                        "Page de carnet de voyage non trouvee avec l'ID: " + pageId));

        if (!page.getProfilEnfant().getId().equals(profilEnfantId)) {
            throw new ResourceNotFoundException(ErrorCode.UNKNOWN_CARNET_VOYAGE,
                    "Page de carnet de voyage non trouvee avec l'ID: " + pageId);
        }

        return page;
    }

    private PageCarnetVoyageResponse toResponse(PageCarnetVoyage page) {
        List<ElementCarnetVoyageResponse> elements = page.getElements().stream()
                .map(e -> new ElementCarnetVoyageResponse(
                        e.getId(),
                        e.getType(),
                        e.getNom(),
                        e.getDescription(),
                        e.getImageUrl(),
                        e.getOrdreAffichage()))
                .toList();

        return new PageCarnetVoyageResponse(
                page.getId(),
                page.getProfilEnfant().getId(),
                page.getConte() != null ? page.getConte().getId() : null,
                page.getPays(),
                page.getDrapeauUrl(),
                page.getLanguesDecouvertes(),
                page.getDateVisite(),
                page.getIdentiteNotes(),
                page.getNatureNotes(),
                page.getSocieteNotes(),
                page.getCultureNotes(),
                page.getExperienceNotes(),
                page.getStatut(),
                page.getFichierExportUrl(),
                elements,
                page.getDateCreation(),
                page.getDateMiseAJour());
    }
}
