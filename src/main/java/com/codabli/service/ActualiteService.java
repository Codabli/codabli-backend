package com.codabli.service;

import com.codabli.dto.ActualiteRequest;
import com.codabli.dto.ActualiteResponse;
import com.codabli.entity.Actualite;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ActualiteRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service metier pour les actualites.
 *
 * Securite :
 * - Lecture (GET) : publique, filtree sur publie=true
 * - Ecriture (POST/PUT/DELETE) : reservee aux admins via @PreAuthorize dans le
 * controller
 */
@Service
@Transactional
public class ActualiteService {

    private final ActualiteRepository actualiteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ActualiteService(ActualiteRepository actualiteRepository,
            UtilisateurRepository utilisateurRepository) {
        this.actualiteRepository = actualiteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — admin only (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public ActualiteResponse creer(ActualiteRequest request, Jwt jwt) {
        Utilisateur auteur = getUtilisateurFromJwt(jwt);

        Actualite actualite = Actualite.builder()
                .auteur(auteur)
                .titre(request.getTitre())
                .imageUrl(request.getImageUrl())
                .resume(request.getResume())
                .contenu(request.getContenu())
                .publie(request.getPublie() != null && request.getPublie())
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
        Actualite actualite = actualiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Actualite non trouvee avec l'ID: " + id));

        actualite.setTitre(request.getTitre());
        actualite.setImageUrl(request.getImageUrl());
        actualite.setResume(request.getResume());
        actualite.setContenu(request.getContenu());

        // Gestion du passage brouillon → publie
        if (request.getPublie() != null) {
            boolean etaitPublie = actualite.isPublie();
            actualite.setPublie(request.getPublie());
            if (!etaitPublie && request.getPublie()) {
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
        Actualite actualite = actualiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Actualite non trouvee avec l'ID: " + id));
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
        Actualite actualite = actualiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Actualite non trouvee avec l'ID: " + id));
        return toResponse(actualite);
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

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    private ActualiteResponse toResponse(Actualite actualite) {
        return ActualiteResponse.builder()
                .id(actualite.getId())
                .titre(actualite.getTitre())
                .imageUrl(actualite.getImageUrl())
                .resume(actualite.getResume())
                .contenu(actualite.getContenu())
                .publie(actualite.isPublie())
                .datePublication(actualite.getDatePublication())
                .dateCreation(actualite.getDateCreation())
                .dateMiseAJour(actualite.getDateMiseAJour())
                .auteurId(actualite.getAuteur().getId())
                .auteurNom(actualite.getAuteur().getNom())
                .auteurPrenom(actualite.getAuteur().getPrenom())
                .build();
    }
}
