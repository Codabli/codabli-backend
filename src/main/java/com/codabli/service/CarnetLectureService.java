package com.codabli.service;

import com.codabli.dto.ElementCarnetLectureRequest;
import com.codabli.dto.ElementCarnetLectureResponse;
import com.codabli.dto.PageCarnetLectureRequest;
import com.codabli.dto.PageCarnetLectureResponse;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ElementCarnetLecture;
import com.codabli.entity.PageCarnetLecture;
import com.codabli.entity.ProfilEnfant;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutPageCarnet;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.PageCarnetLectureRepository;
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
 * Service metier pour le Carnet de Lecture (CDC 8.17.1, CDL-01 a 17).
 *
 * Securite (RG-13, confidentialite des carnets) : seul le responsable
 * (parent/professionnel_education) proprietaire du profil enfant peut
 * consulter/modifier son carnet, ou un admin/super_admin.
 *
 * RG-10 : le backend ne genere jamais le resume, les reponses ou les
 * descriptions a la place de l'enfant — il se contente de sauvegarder ce
 * qui est saisi, et de prerempli uniquement les metadonnees objectives du
 * conte (titre, auteur, couverture, langue) au moment de la creation.
 */
@Service
@Transactional
public class CarnetLectureService {

    private final PageCarnetLectureRepository pageRepository;
    private final ProfilEnfantRepository profilEnfantRepository;
    private final ConteDanseRepository conteDanseRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CarnetLectureService(PageCarnetLectureRepository pageRepository,
            ProfilEnfantRepository profilEnfantRepository,
            ConteDanseRepository conteDanseRepository,
            UtilisateurRepository utilisateurRepository) {
        this.pageRepository = pageRepository;
        this.profilEnfantRepository = profilEnfantRepository;
        this.conteDanseRepository = conteDanseRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER UNE PAGE — CDL-01
    // ────────────────────────────────────────────────────────────────

    public PageCarnetLectureResponse creerPage(UUID profilEnfantId, PageCarnetLectureRequest request, Jwt jwt) {
        ProfilEnfant profil = getProfilAvecDroit(profilEnfantId, jwt);

        PageCarnetLecture page = PageCarnetLecture.builder()
                .profilEnfant(profil)
                .titre(request.getTitre())
                .auteur(request.getAuteur())
                .couvertureUrl(request.getCouvertureUrl())
                .langue(request.getLangue())
                .dateLecture(request.getDateLecture() != null ? request.getDateLecture() : LocalDate.now())
                .theme(request.getTheme())
                .resume(request.getResume())
                .motsPreferes(request.getMotsPreferes())
                .questionsReponses(request.getQuestionsReponses())
                .build();

        if (request.getConteId() != null) {
            ConteDanse conte = conteDanseRepository.findById(request.getConteId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conte danse non trouve avec l'ID: " + request.getConteId()));
            page.setConte(conte);
            if (page.getTitre() == null) {
                page.setTitre(conte.getTitre());
            }
            if (page.getAuteur() == null) {
                page.setAuteur(conte.getCreateur().getPrenom() + " " + conte.getCreateur().getNom());
            }
            if (page.getCouvertureUrl() == null) {
                page.setCouvertureUrl(conte.getCouvertureUrl());
            }
            if (page.getLangue() == null) {
                page.setLangue(conte.getLangueOriginale());
            }
        }

        PageCarnetLecture saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // CONSULTER LE CARNET — CDL-17
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PageCarnetLectureResponse> listerPages(UUID profilEnfantId, Jwt jwt) {
        getProfilAvecDroit(profilEnfantId, jwt);
        return pageRepository.findByProfilEnfantIdOrderByDateCreationDesc(profilEnfantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // PREVISUALISER UNE PAGE — CDL-14
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageCarnetLectureResponse getPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        return toResponse(getPageAvecDroit(profilEnfantId, pageId, jwt));
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER UNE PAGE
    // ────────────────────────────────────────────────────────────────

    public PageCarnetLectureResponse modifierPage(UUID profilEnfantId, UUID pageId,
            PageCarnetLectureRequest request, Jwt jwt) {
        PageCarnetLecture page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        if (page.getStatut() == StatutPageCarnet.terminee) {
            // RG-18 : une page terminee reste modifiable tant qu'elle n'a pas
            // ete transmise pour publication — ici, aucune notion de
            // transmission n'existe encore, donc la modification reste
            // autorisee. On repasse simplement en_cours si du contenu change.
            page.setStatut(StatutPageCarnet.en_cours);
        } else if (page.getStatut() == StatutPageCarnet.non_commencee) {
            page.setStatut(StatutPageCarnet.en_cours);
        }

        page.setTitre(request.getTitre());
        page.setAuteur(request.getAuteur());
        page.setCouvertureUrl(request.getCouvertureUrl());
        page.setLangue(request.getLangue());
        page.setDateLecture(request.getDateLecture());
        page.setTheme(request.getTheme());
        page.setResume(request.getResume());
        page.setMotsPreferes(request.getMotsPreferes());
        page.setQuestionsReponses(request.getQuestionsReponses());

        PageCarnetLecture saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // TERMINER UNE PAGE — CDL-15
    // ────────────────────────────────────────────────────────────────

    public PageCarnetLectureResponse terminerPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        PageCarnetLecture page = getPageAvecDroit(profilEnfantId, pageId, jwt);
        page.setStatut(StatutPageCarnet.terminee);
        PageCarnetLecture saved = pageRepository.save(page);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER UNE PAGE
    // ────────────────────────────────────────────────────────────────

    public void supprimerPage(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        PageCarnetLecture page = getPageAvecDroit(profilEnfantId, pageId, jwt);
        pageRepository.delete(page);
    }

    // ────────────────────────────────────────────────────────────────
    // ELEMENTS (personnages/lieux/objets/creations) — CDL-04/05/06/09/11
    // ────────────────────────────────────────────────────────────────

    public PageCarnetLectureResponse ajouterElement(UUID profilEnfantId, UUID pageId,
            ElementCarnetLectureRequest request, Jwt jwt) {
        PageCarnetLecture page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        ElementCarnetLecture element = ElementCarnetLecture.builder()
                .page(page)
                .type(request.getType())
                .nom(request.getNom())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .ordreAffichage(request.getOrdreAffichage() != null ? request.getOrdreAffichage() : page.getElements().size())
                .build();

        page.getElements().add(element);
        PageCarnetLecture saved = pageRepository.save(page);
        return toResponse(saved);
    }

    public PageCarnetLectureResponse supprimerElement(UUID profilEnfantId, UUID pageId, UUID elementId, Jwt jwt) {
        PageCarnetLecture page = getPageAvecDroit(profilEnfantId, pageId, jwt);

        boolean supprime = page.getElements().removeIf(e -> e.getId().equals(elementId));
        if (!supprime) {
            throw new ResourceNotFoundException("Element non trouve avec l'ID: " + elementId);
        }

        PageCarnetLecture saved = pageRepository.save(page);
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

    private PageCarnetLecture getPageAvecDroit(UUID profilEnfantId, UUID pageId, Jwt jwt) {
        getProfilAvecDroit(profilEnfantId, jwt);

        PageCarnetLecture page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Page de carnet de lecture non trouvee avec l'ID: " + pageId));

        if (!page.getProfilEnfant().getId().equals(profilEnfantId)) {
            throw new ResourceNotFoundException(
                    "Page de carnet de lecture non trouvee avec l'ID: " + pageId);
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

    private PageCarnetLectureResponse toResponse(PageCarnetLecture page) {
        List<ElementCarnetLectureResponse> elements = page.getElements().stream()
                .map(e -> ElementCarnetLectureResponse.builder()
                        .id(e.getId())
                        .type(e.getType())
                        .nom(e.getNom())
                        .description(e.getDescription())
                        .imageUrl(e.getImageUrl())
                        .ordreAffichage(e.getOrdreAffichage())
                        .build())
                .collect(Collectors.toList());

        return PageCarnetLectureResponse.builder()
                .id(page.getId())
                .profilEnfantId(page.getProfilEnfant().getId())
                .conteId(page.getConte() != null ? page.getConte().getId() : null)
                .titre(page.getTitre())
                .auteur(page.getAuteur())
                .couvertureUrl(page.getCouvertureUrl())
                .langue(page.getLangue())
                .dateLecture(page.getDateLecture())
                .theme(page.getTheme())
                .resume(page.getResume())
                .motsPreferes(page.getMotsPreferes())
                .questionsReponses(page.getQuestionsReponses())
                .statut(page.getStatut())
                .fichierExportUrl(page.getFichierExportUrl())
                .elements(elements)
                .dateCreation(page.getDateCreation())
                .dateMiseAJour(page.getDateMiseAJour())
                .build();
    }
}
