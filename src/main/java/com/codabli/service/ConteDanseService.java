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
import java.util.UUID;

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
