package com.codabli.service;

import com.codabli.dto.ConteDanseTraductionRequest;
import com.codabli.dto.ConteDanseTraductionResponse;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.ConteDanseTraductionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les traductions de Contes Danses (CDC 8.5, LAN-01/02/03).
 *
 * Securite : la gestion des traductions (creation/modification/statut) est
 * reservee aux roles traducteur/comite_lecture/admin/super_admin via
 * @PreAuthorize dans le controller. La lecture publique ne retourne que les
 * traductions au statut "publiee" (LAN-03).
 */
@Service
@Transactional
public class ConteDanseTraductionService {

    private final ConteDanseTraductionRepository traductionRepository;
    private final ConteDanseRepository conteDanseRepository;
    private final EntityManager entityManager;

    public ConteDanseTraductionService(ConteDanseTraductionRepository traductionRepository,
            ConteDanseRepository conteDanseRepository,
            EntityManager entityManager) {
        this.traductionRepository = traductionRepository;
        this.conteDanseRepository = conteDanseRepository;
        this.entityManager = entityManager;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les traductions publiees (LAN-01/03)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseTraductionResponse> listerPubliees(UUID conteId) {
        return traductionRepository.findByConteIdAndStatut(conteId, StatutTraduction.publiee)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER TOUT — traducteur/comite_lecture/admin (LAN-02)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseTraductionResponse> listerToutes(UUID conteId) {
        return traductionRepository.findByConteId(conteId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // CREER
    // ────────────────────────────────────────────────────────────────

    public ConteDanseTraductionResponse creer(UUID conteId, ConteDanseTraductionRequest request) {
        ConteDanse conte = conteDanseRepository.findById(conteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conte danse non trouve avec l'ID: " + conteId));

        ConteDanseTraduction traduction = ConteDanseTraduction.builder()
                .conte(conte)
                .langue(request.getLangue())
                .variante(request.getVariante())
                .texte(request.getTexte())
                .audioUrl(request.getAudioUrl())
                .videoUrl(request.getVideoUrl())
                .sousTitresUrl(request.getSousTitresUrl())
                .build();

        appliquerIntervenants(traduction, request);

        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER
    // ────────────────────────────────────────────────────────────────

    public ConteDanseTraductionResponse modifier(UUID id, ConteDanseTraductionRequest request) {
        ConteDanseTraduction traduction = getOrThrow(id);

        traduction.setLangue(request.getLangue());
        traduction.setVariante(request.getVariante());
        traduction.setTexte(request.getTexte());
        traduction.setAudioUrl(request.getAudioUrl());
        traduction.setVideoUrl(request.getVideoUrl());
        traduction.setSousTitresUrl(request.getSousTitresUrl());

        appliquerIntervenants(traduction, request);

        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // CHANGER LE STATUT — LAN-02/03
    // ────────────────────────────────────────────────────────────────

    /**
     * Change le statut d'une traduction. Regle LAN-03 : seules les versions
     * deja "validee" (ou deja publiees) peuvent passer a "publiee".
     */
    public ConteDanseTraductionResponse changerStatut(UUID id, StatutTraduction nouveauStatut) {
        ConteDanseTraduction traduction = getOrThrow(id);

        if (nouveauStatut == StatutTraduction.publiee
                && traduction.getStatut() != StatutTraduction.validee
                && traduction.getStatut() != StatutTraduction.publiee) {
            throw new IllegalStateException(
                    "Seules les traductions validees peuvent etre publiees (LAN-03)");
        }

        traduction.setStatut(nouveauStatut);
        ConteDanseTraduction saved = traductionRepository.save(traduction);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id) {
        ConteDanseTraduction traduction = getOrThrow(id);
        traductionRepository.delete(traduction);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private void appliquerIntervenants(ConteDanseTraduction traduction, ConteDanseTraductionRequest request) {
        if (request.getTraducteurId() != null) {
            traduction.setTraducteur(entityManager.getReference(Utilisateur.class, request.getTraducteurId()));
        }
        if (request.getRelecteurId() != null) {
            traduction.setRelecteur(entityManager.getReference(Utilisateur.class, request.getRelecteurId()));
        }
    }

    private ConteDanseTraduction getOrThrow(UUID id) {
        return traductionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Traduction non trouvee avec l'ID: " + id));
    }

    private ConteDanseTraductionResponse toResponse(ConteDanseTraduction traduction) {
        return ConteDanseTraductionResponse.builder()
                .id(traduction.getId())
                .conteId(traduction.getConte().getId())
                .langue(traduction.getLangue())
                .variante(traduction.getVariante())
                .texte(traduction.getTexte())
                .audioUrl(traduction.getAudioUrl())
                .videoUrl(traduction.getVideoUrl())
                .sousTitresUrl(traduction.getSousTitresUrl())
                .traducteurId(traduction.getTraducteur() != null ? traduction.getTraducteur().getId() : null)
                .traducteurNom(traduction.getTraducteur() != null ? traduction.getTraducteur().getNom() : null)
                .relecteurId(traduction.getRelecteur() != null ? traduction.getRelecteur().getId() : null)
                .relecteurNom(traduction.getRelecteur() != null ? traduction.getRelecteur().getNom() : null)
                .statut(traduction.getStatut())
                .dateCreation(traduction.getDateCreation())
                .dateMiseAJour(traduction.getDateMiseAJour())
                .build();
    }
}
