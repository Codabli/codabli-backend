package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.entity.Classe;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Service metier pour les contes danses.
 *
 * Securite a deux niveaux :
 * 1. Niveau role (grossier) : gere par @PreAuthorize dans le controller
 * 2. Niveau relation (fin) : gere ICI — seul l'auteur ou un admin peut
 * modifier/supprimer
 */
@Service
@Transactional
public class ConteDanseService {

    private final ConteDanseRepository conteDanseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityManager entityManager;

    public ConteDanseService(ConteDanseRepository conteDanseRepository,
            UtilisateurRepository utilisateurRepository,
            EntityManager entityManager) {
        this.conteDanseRepository = conteDanseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entityManager = entityManager;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — utilisateur authentifie (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public ConteDanseResponse creer(ConteDanseRequest request, Jwt jwt) {
        Utilisateur createur = getUtilisateurFromJwt(jwt);

        ConteDanse conte = ConteDanse.builder()
                .createur(createur)
                .titre(request.getTitre())
                .description(request.getDescription())
                .thematique(request.getThematique())
                .langueOriginale(request.getLangueOriginale())
                .couvertureUrl(request.getCouvertureUrl())
                .pays(request.getPays())
                .culture(request.getCulture())
                .ageMin(request.getAgeMin())
                .ageMax(request.getAgeMax())
                .dureeMinutes(request.getDureeMinutes())
                .credits(request.getCredits())
                .fichierTexteUrl(request.getFichierTexteUrl())
                .fichierAudioUrl(request.getFichierAudioUrl())
                .fichierVideoUrl(request.getFichierVideoUrl())
                .build();

        // Rattacher a une ecole si ecoleId est fourni
        if (request.getEcoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.getEcoleId());
            conte.setEcole(ecole);
        }

        // Rattacher a une classe si classeId est fourni
        if (request.getClasseId() != null) {
            Classe classe = entityManager.getReference(Classe.class, request.getClasseId());
            conte.setClasse(classe);
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — auteur ou admin (verification fine dans le service)
    // ────────────────────────────────────────────────────────────────

    public ConteDanseResponse modifier(UUID id, ConteDanseRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        ConteDanse conte = conteDanseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conte danse non trouve avec l'ID: " + id));

        // Verification fine : seul l'auteur ou un admin peut modifier
        verifierAuteurOuAdmin(conte, utilisateur, jwt);

        conte.setTitre(request.getTitre());
        conte.setDescription(request.getDescription());
        conte.setThematique(request.getThematique());
        conte.setCouvertureUrl(request.getCouvertureUrl());
        conte.setPays(request.getPays());
        conte.setCulture(request.getCulture());
        conte.setAgeMin(request.getAgeMin());
        conte.setAgeMax(request.getAgeMax());
        conte.setDureeMinutes(request.getDureeMinutes());
        conte.setCredits(request.getCredits());
        conte.setFichierTexteUrl(request.getFichierTexteUrl());
        conte.setFichierAudioUrl(request.getFichierAudioUrl());
        conte.setFichierVideoUrl(request.getFichierVideoUrl());

        if (request.getLangueOriginale() != null) {
            conte.setLangueOriginale(request.getLangueOriginale());
        }

        if (request.getEcoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.getEcoleId());
            conte.setEcole(ecole);
        }

        if (request.getClasseId() != null) {
            Classe classe = entityManager.getReference(Classe.class, request.getClasseId());
            conte.setClasse(classe);
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // WORKFLOW DE MODERATION (RG-01) — soumettre / valider / refuser
    //
    // Cycle : brouillon --soumettre--> en_revision_enseignant
    //         --valider(enseignant)--> en_revision_comite
    //         --valider(comite_lecture)--> publie
    // admin/super_admin ont un droit total : valider() publie directement
    // depuis n'importe quel statut. refuser() est possible a tout moment
    // par un role habilite.
    // ────────────────────────────────────────────────────────────────

    /**
     * L'auteur soumet son brouillon (ou un conte refuse corrige) a la
     * moderation.
     */
    public ConteDanseResponse soumettre(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        ConteDanse conte = getOrThrow(id);

        if (!conte.getCreateur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Seul l'auteur peut soumettre son conte a la moderation");
        }
        if (conte.getStatut() != StatutConte.brouillon && conte.getStatut() != StatutConte.refuse) {
            throw new IllegalStateException(
                    "Seul un conte en brouillon ou refuse peut etre soumis a la moderation");
        }

        conte.setStatut(StatutConte.en_revision_enseignant);
        conte.setMotifModeration(null);
        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    /**
     * Fait progresser un conte dans le cycle de validation :
     * - enseignant : en_revision_enseignant -> en_revision_comite (limite a
     * ses propres classes si le conte est rattache a une classe)
     * - comite_lecture : en_revision_comite -> publie
     * - admin/super_admin : droit total, publie directement
     */
    public ConteDanseResponse valider(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);
        ConteDanse conte = getOrThrow(id);

        if (roles.contains("admin") || roles.contains("super_admin")) {
            publier(conte, utilisateur);
        } else if (roles.contains("comite_lecture")) {
            if (conte.getStatut() != StatutConte.en_revision_comite) {
                throw new IllegalStateException(
                        "Ce conte n'est pas en attente de validation du comite de lecture");
            }
            publier(conte, utilisateur);
        } else if (roles.contains("enseignant")) {
            if (conte.getStatut() != StatutConte.en_revision_enseignant) {
                throw new IllegalStateException(
                        "Ce conte n'est pas en attente de validation enseignant");
            }
            if (conte.getClasse() != null
                    && !conte.getClasse().getEnseignant().getId().equals(utilisateur.getId())) {
                throw new AccessDeniedException("Vous ne pouvez valider que les contes de vos classes");
            }
            conte.setValideParEnseignant(utilisateur);
            conte.setStatut(StatutConte.en_revision_comite);
        } else {
            throw new AccessDeniedException("Role non autorise a valider un conte danse");
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    /**
     * Refuse un conte, quel que soit son statut de revision en cours (MOD-03,
     * le motif est enregistre).
     */
    public ConteDanseResponse refuser(UUID id, String motif, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);
        ConteDanse conte = getOrThrow(id);

        boolean autorise = roles.contains("admin") || roles.contains("super_admin")
                || roles.contains("comite_lecture");
        if (!autorise && roles.contains("enseignant") && conte.getStatut() == StatutConte.en_revision_enseignant
                && (conte.getClasse() == null
                        || conte.getClasse().getEnseignant().getId().equals(utilisateur.getId()))) {
            autorise = true;
        }
        if (!autorise) {
            throw new AccessDeniedException("Vous n'etes pas autorise a refuser ce conte danse");
        }

        conte.setStatut(StatutConte.refuse);
        conte.setMotifModeration(motif);
        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MES CONTES — l'auteur retrouve ses propres contes, quel que soit
    // leur statut (brouillon, en revision, publie, refuse)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseResponse> mesContes(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return conteDanseRepository.findByCreateurId(utilisateur.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // FILE D'ATTENTE DE MODERATION — selon le role de l'appelant
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseResponse> listerEnAttente(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        List<ConteDanse> contes;
        if (roles.contains("admin") || roles.contains("super_admin")) {
            contes = conteDanseRepository.findByStatutIn(
                    List.of(StatutConte.en_revision_enseignant, StatutConte.en_revision_comite));
        } else if (roles.contains("comite_lecture")) {
            contes = conteDanseRepository.findByStatut(StatutConte.en_revision_comite);
        } else if (roles.contains("enseignant")) {
            contes = conteDanseRepository.findByStatutAndEnseignant(
                    StatutConte.en_revision_enseignant, utilisateur.getId());
        } else {
            throw new AccessDeniedException("Role non autorise a consulter la file de moderation");
        }

        return contes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void publier(ConteDanse conte, Utilisateur validateur) {
        conte.setStatut(StatutConte.publie);
        conte.setValideParComite(validateur);
        conte.setDatePublication(OffsetDateTime.now());
        conte.setMotifModeration(null);
    }

    private ConteDanse getOrThrow(UUID id) {
        return conteDanseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conte danse non trouve avec l'ID: " + id));
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — auteur ou admin
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        ConteDanse conte = conteDanseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conte danse non trouve avec l'ID: " + id));

        verifierAuteurOuAdmin(conte, utilisateur, jwt);
        conteDanseRepository.delete(conte);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les publies
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> listerPublies(Pageable pageable) {
        return conteDanseRepository
                .findByStatutOrderByDatePublicationDesc(StatutConte.publie, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // FILTRER — public, CON-03 (age, langue, pays, thematique, acces)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> filtrerPublies(String langue, String pays, String thematique,
            AccesConte acces, Integer age, Pageable pageable) {
        return conteDanseRepository
                .filtrerPublies(StatutConte.publie, langue, pays, thematique, acces, age, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ConteDanseResponse getById(UUID id) {
        ConteDanse conte = conteDanseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conte danse non trouve avec l'ID: " + id));
        return toResponse(conte);
    }

    // ────────────────────────────────────────────────────────────────
    // RECHERCHE — utilisee par RechercheController
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> rechercher(String query, Pageable pageable) {
        return conteDanseRepository
                .rechercherPublies(StatutConte.publie, query, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    /**
     * Verifie que l'utilisateur est l'auteur du conte ou un admin.
     * Leve AccessDeniedException sinon.
     */
    private void verifierAuteurOuAdmin(ConteDanse conte, Utilisateur utilisateur, Jwt jwt) {
        Collection<String> roles = extractRoles(jwt);
        boolean estAuteur = conte.getCreateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException(
                    "Vous ne pouvez modifier/supprimer que vos propres contes danses");
        }
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

    private ConteDanseResponse toResponse(ConteDanse conte) {
        return ConteDanseResponse.builder()
                .id(conte.getId())
                .titre(conte.getTitre())
                .description(conte.getDescription())
                .thematique(conte.getThematique())
                .langueOriginale(conte.getLangueOriginale())
                .couvertureUrl(conte.getCouvertureUrl())
                .pays(conte.getPays())
                .culture(conte.getCulture())
                .ageMin(conte.getAgeMin())
                .ageMax(conte.getAgeMax())
                .dureeMinutes(conte.getDureeMinutes())
                .credits(conte.getCredits())
                .statut(conte.getStatut())
                .acces(conte.getAcces())
                .isbn(conte.getIsbn())
                .fichierTexteUrl(conte.getFichierTexteUrl())
                .fichierAudioUrl(conte.getFichierAudioUrl())
                .fichierVideoUrl(conte.getFichierVideoUrl())
                .motifModeration(conte.getMotifModeration())
                .dateCreation(conte.getDateCreation())
                .datePublication(conte.getDatePublication())
                .createurId(conte.getCreateur().getId())
                .createurNom(conte.getCreateur().getNom())
                .createurPrenom(conte.getCreateur().getPrenom())
                .ecoleId(conte.getEcole() != null ? conte.getEcole().getId() : null)
                .classeId(conte.getClasse() != null ? conte.getClasse().getId() : null)
                .build();
    }
}
