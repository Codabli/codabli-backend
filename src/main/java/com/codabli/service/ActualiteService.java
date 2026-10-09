package com.codabli.service;

import com.codabli.dto.ActualiteRequest;
import com.codabli.dto.ActualiteResponse;
import com.codabli.entity.Actualite;
import com.codabli.entity.Utilisateur;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ActualiteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service metier pour les actualites.
 * <p>
 * Securite :
 * - Lecture (GET) : publique, filtree sur publie=true
 * - Ecriture (POST/PUT/DELETE) : reservee aux admins via @PreAuthorize dans le
 * controller
 */
@Service
@Transactional
public class ActualiteService {

    private final ActualiteRepository actualiteRepository;

    private final UtilisateurService utilisateurService;

    public ActualiteService(ActualiteRepository actualiteRepository, UtilisateurService utilisateurService) {
        this.actualiteRepository = actualiteRepository;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — admin only (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public ActualiteResponse creer(ActualiteRequest request, Jwt jwt) {
        Utilisateur auteur = utilisateurService.getUtilisateurFromJwt(jwt);

        Actualite actualite = Actualite.builder()
                .auteur(auteur)
                .titre(request.titre())
                .imageUrl(request.imageUrl())
                .resume(request.resume())
                .contenu(request.contenu())
                .publie(request.publie() != null && request.publie())
                .build();

        // Si publie immediatement, fixer la date de publication
        if (actualite.isPublie()) {
            actualite.setDatePublication(OffsetDateTime.now());
        }

        Actualite saved = actualiteRepository.save(actualite);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — admin only (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public ActualiteResponse modifier(UUID id, ActualiteRequest request, Jwt jwt) {
        Actualite actualite = findById(id);

        actualite.setTitre(request.titre());
        actualite.setImageUrl(request.imageUrl());
        actualite.setResume(request.resume());
        actualite.setContenu(request.contenu());

        // Gestion du passage brouillon → publie
        if (request.publie() != null) {
            boolean etaitPublie = actualite.isPublie();
            actualite.setPublie(request.publie());
            if (!etaitPublie && request.publie()) {
                actualite.setDatePublication(OffsetDateTime.now());
            }
        }

        Actualite saved = actualiteRepository.save(actualite);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — admin only (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id) {
        Actualite actualite = findById(id);
        actualiteRepository.delete(actualite);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les publiees
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ActualiteResponse> listerPubliees(Pageable pageable) {
        return actualiteRepository.findByPublieTrueOrderByDatePublicationDesc(pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ActualiteResponse getById(UUID id) {
        Actualite actualite = findById(id);
        return toResponse(actualite);
    }

    private Actualite findById(UUID id) {
        return actualiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_ACTUALITE,
                        "Actualite non trouvee avec l'ID: " + id));
    }

    // ────────────────────────────────────────────────────────────────
    // RECHERCHE — utilisee par RechercheController
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ActualiteResponse> rechercher(String query, Pageable pageable) {
        return actualiteRepository.rechercherPubliees(query, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────
    private ActualiteResponse toResponse(Actualite actualite) {
        return new ActualiteResponse(
                actualite.getId(),
                actualite.getTitre(),
                actualite.getImageUrl(),
                actualite.getResume(),
                actualite.getContenu(),
                actualite.isPublie(),
                actualite.getDatePublication(),
                actualite.getDateCreation(),
                actualite.getDateMiseAJour(),
                actualite.getAuteur().getId(),
                actualite.getAuteur().getNom(),
                actualite.getAuteur().getPrenom());
    }
}
