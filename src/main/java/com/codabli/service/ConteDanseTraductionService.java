package com.codabli.service;

import com.codabli.dto.ConteDanseTraductionRequest;
import com.codabli.dto.ConteDanseTraductionResponse;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ConteDanseTraductionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les traductions de Contes Danses (CDC 8.5, LAN-01/02/03).
 * <p>
 * Securite : la gestion des traductions (creation/modification/statut) est
 * reservee aux roles traducteur/comite_lecture/admin/super_admin via
 * @PreAuthorize dans le controller. La lecture publique ne retourne que les
 * traductions au statut "publiee" (LAN-03).
 */
@Service
@Transactional
public class ConteDanseTraductionService {

    private final ConteDanseTraductionRepository traductionRepository;
    private final EntityManager entityManager;
    private final ConteDanseService conteDanseService;
    private final UtilisateurService utilisateurService;

    public ConteDanseTraductionService(ConteDanseTraductionRepository traductionRepository,
                                       EntityManager entityManager,
                                       ConteDanseService conteDanseService,
                                       UtilisateurService utilisateurService) {
        this.traductionRepository = traductionRepository;
        this.entityManager = entityManager;
        this.conteDanseService = conteDanseService;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les traductions publiees (LAN-01/03)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseTraductionResponse> listerPubliees(UUID conteId) {
        return traductionRepository.findByConteIdAndStatut(conteId, StatutTraduction.publiee)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER TOUT — traducteur/comite_lecture/admin (LAN-02)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseTraductionResponse> listerToutes(UUID conteId) {
        conteDanseService.findById(conteId);
        return traductionRepository.findByConteId(conteId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // CREER
    // ────────────────────────────────────────────────────────────────

    public ConteDanseTraductionResponse creer(UUID conteId, ConteDanseTraductionRequest request) {
        ConteDanse conte = conteDanseService.findById(conteId);

        ConteDanseTraduction traduction = ConteDanseTraduction.builder()
                .conte(conte)
                .langue(request.langue())
                .variante(request.variante())
                .texte(request.texte())
                .audioUrl(request.audioUrl())
                .videoUrl(request.videoUrl())
                .sousTitresUrl(request.sousTitresUrl())
                .build();

        appliquerIntervenants(traduction, request);

        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER
    // ────────────────────────────────────────────────────────────────

    public ConteDanseTraductionResponse modifier(UUID conteId, UUID id, ConteDanseTraductionRequest request) {
        ConteDanseTraduction traduction = findById(conteId, id);

        traduction.setLangue(request.langue());
        traduction.setVariante(request.variante());
        traduction.setTexte(request.texte());
        traduction.setAudioUrl(request.audioUrl());
        traduction.setVideoUrl(request.videoUrl());
        traduction.setSousTitresUrl(request.sousTitresUrl());

        appliquerIntervenants(traduction, request);
        traduction.setStatut(StatutTraduction.a_traduire);

        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // CHANGER LE STATUT — LAN-02/03
    // ────────────────────────────────────────────────────────────────

    /**
     * Applique une transition du cycle de vie, en distinguant les actions du
     * traducteur de celles du comite de lecture.
     */
    public ConteDanseTraductionResponse changerStatut(UUID conteId,
                                                      UUID id,
                                                      StatutTraduction nouveauStatut,
                                                      Jwt jwt) {
        ConteDanseTraduction traduction = findById(conteId, id);
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        boolean traducteur = roles.contains("traducteur");
        boolean relecteur = roles.contains("comite_lecture")
                || roles.contains("admin")
                || roles.contains("super_admin");

        if (!traducteur && !relecteur) {
            throw new AccessDeniedException(
                    "Vous n'etes pas autorise a changer le statut d'une traduction");
        }

        verifierTransitionAutorisee(traduction.getStatut(), nouveauStatut, relecteur);
        traduction.setStatut(nouveauStatut);
        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    private void verifierTransitionAutorisee(StatutTraduction statutActuel,
                                             StatutTraduction nouveauStatut,
                                             boolean relecteur) {
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le nouveau statut est obligatoire");
        }

        if (statutActuel == nouveauStatut) {
            return;
        }

        boolean transitionAutorisee = switch (statutActuel) {
            case a_traduire -> nouveauStatut == StatutTraduction.en_cours;
            case en_cours -> nouveauStatut == StatutTraduction.relecture;
            case relecture -> nouveauStatut == StatutTraduction.en_cours
                    || (relecteur && nouveauStatut == StatutTraduction.validee);
            case validee -> relecteur && (nouveauStatut == StatutTraduction.publiee
                    || nouveauStatut == StatutTraduction.relecture);
            case publiee -> relecteur && nouveauStatut == StatutTraduction.archivee;
            case archivee -> false;
        };

        if (!transitionAutorisee) {
            throw new BusinessException(ErrorCode.INVALID_TRANSLATION_TRANSITION,
                    "Transition de statut non autorisee : " + statutActuel + " -> " + nouveauStatut);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID conteId, UUID id) {
        ConteDanseTraduction traduction = findById(conteId, id);
        traductionRepository.delete(traduction);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private void appliquerIntervenants(ConteDanseTraduction traduction, ConteDanseTraductionRequest request) {
        traduction.setTraducteur(request.traducteurId() == null
                ? null
                : entityManager.getReference(Utilisateur.class, request.traducteurId()));
        traduction.setRelecteur(request.relecteurId() == null
                ? null
                : entityManager.getReference(Utilisateur.class, request.relecteurId()));
    }

    private ConteDanseTraduction findById(UUID conteId, UUID id) {
        return traductionRepository.findById(id)
                .filter(traduction -> traduction.getConte().getId().equals(conteId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_CONTE_TRADUCTION,
                        "Traduction non trouvee avec l'ID: " + id));
    }

    private ConteDanseTraductionResponse toResponse(ConteDanseTraduction traduction) {
        return new ConteDanseTraductionResponse(
                traduction.getId(),
                traduction.getConte().getId(),
                traduction.getLangue(),
                traduction.getVariante(),
                traduction.getTexte(),
                traduction.getAudioUrl(),
                traduction.getVideoUrl(),
                traduction.getSousTitresUrl(),
                traduction.getTraducteur() != null ? traduction.getTraducteur().getId() : null,
                traduction.getTraducteur() != null ? traduction.getTraducteur().getNom() : null,
                traduction.getRelecteur() != null ? traduction.getRelecteur().getId() : null,
                traduction.getRelecteur() != null ? traduction.getRelecteur().getNom() : null,
                traduction.getStatut(),
                traduction.getDateCreation(),
                traduction.getDateMiseAJour());
    }
}
