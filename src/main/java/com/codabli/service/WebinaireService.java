package com.codabli.service;

import com.codabli.dto.InscriptionWebinaireResponse;
import com.codabli.dto.WebinaireRequest;
import com.codabli.dto.WebinaireResponse;
import com.codabli.entity.InscriptionWebinaire;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.Webinaire;
import com.codabli.repository.InscriptionWebinaireRepository;
import com.codabli.repository.UtilisateurRepository;
import com.codabli.repository.WebinaireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les webinaires (CDC PRO-06).
 */
@Service
@Transactional
public class WebinaireService {

    private final WebinaireRepository webinaireRepository;
    private final InscriptionWebinaireRepository inscriptionRepository;
    private final UtilisateurRepository utilisateurRepository;

    public WebinaireService(WebinaireRepository webinaireRepository,
                            InscriptionWebinaireRepository inscriptionRepository,
                            UtilisateurRepository utilisateurRepository) {
        this.webinaireRepository = webinaireRepository;
        this.inscriptionRepository = inscriptionRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CATALOGUE — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<WebinaireResponse> lister(Pageable pageable) {
        return webinaireRepository.findByActifTrueOrderByDateDebutAsc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WebinaireResponse getById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    // ────────────────────────────────────────────────────────────────
    // GESTION — admin/super_admin
    // ────────────────────────────────────────────────────────────────

    public WebinaireResponse creer(WebinaireRequest request) {
        Webinaire webinaire = Webinaire.builder()
                .titre(request.titre())
                .description(request.description())
                .intervenant(request.intervenant())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .lienUrl(request.lienUrl())
                .lienRediffusionUrl(request.lienRediffusionUrl())
                .documentsUrl(request.documentsUrl())
                .capaciteMax(request.capaciteMax())
                .actif(request.actif() == null || request.actif())
                .build();

        Webinaire saved = webinaireRepository.save(webinaire);
        return toResponse(saved);
    }

    public WebinaireResponse modifier(UUID id, WebinaireRequest request) {
        Webinaire webinaire = getOrThrow(id);

        webinaire.setTitre(request.titre());
        webinaire.setDescription(request.description());
        webinaire.setIntervenant(request.intervenant());
        webinaire.setDateDebut(request.dateDebut());
        webinaire.setDateFin(request.dateFin());
        webinaire.setLienUrl(request.lienUrl());
        webinaire.setLienRediffusionUrl(request.lienRediffusionUrl());
        webinaire.setDocumentsUrl(request.documentsUrl());
        webinaire.setCapaciteMax(request.capaciteMax());
        if (request.actif() != null) {
            webinaire.setActif(request.actif());
        }

        Webinaire saved = webinaireRepository.save(webinaire);
        return toResponse(saved);
    }

    public void supprimer(UUID id) {
        webinaireRepository.delete(getOrThrow(id));
    }

    // ────────────────────────────────────────────────────────────────
    // INSCRIPTION — utilisateur authentifie
    // ────────────────────────────────────────────────────────────────

    public InscriptionWebinaireResponse sInscrire(UUID webinaireId, Jwt jwt) {
        Webinaire webinaire = getOrThrow(webinaireId);
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);

        if (inscriptionRepository.findByWebinaireIdAndUtilisateurId(webinaireId, utilisateur.getId()).isPresent()) {
            throw new IllegalStateException("Vous etes deja inscrit a ce webinaire");
        }

        if (webinaire.getCapaciteMax() != null
                && inscriptionRepository.countByWebinaireId(webinaireId) >= webinaire.getCapaciteMax()) {
            throw new IllegalStateException("La capacite maximale de ce webinaire est atteinte");
        }

        InscriptionWebinaire inscription = InscriptionWebinaire.builder()
                .webinaire(webinaire)
                .utilisateur(utilisateur)
                .build();

        InscriptionWebinaire saved = inscriptionRepository.save(inscription);
        return toInscriptionResponse(saved);
    }

    public void seDesinscrire(UUID webinaireId, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        InscriptionWebinaire inscription = inscriptionRepository
                .findByWebinaireIdAndUtilisateurId(webinaireId, utilisateur.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvee"));
        inscriptionRepository.delete(inscription);
    }

    @Transactional(readOnly = true)
    public List<InscriptionWebinaireResponse> mesInscriptions(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return inscriptionRepository.findByUtilisateurIdOrderByDateInscriptionDesc(utilisateur.getId()).stream()
                .map(this::toInscriptionResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private Webinaire getOrThrow(UUID id) {
        return webinaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webinaire non trouve avec l'ID: " + id));
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        return utilisateurRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + jwt.getSubject()));
    }

    private WebinaireResponse toResponse(Webinaire webinaire) {
        return new WebinaireResponse(
                webinaire.getId(),
                webinaire.getTitre(),
                webinaire.getDescription(),
                webinaire.getIntervenant(),
                webinaire.getDateDebut(),
                webinaire.getDateFin(),
                webinaire.getLienUrl(),
                webinaire.getLienRediffusionUrl(),
                webinaire.getDocumentsUrl(),
                webinaire.getCapaciteMax(),
                inscriptionRepository.countByWebinaireId(webinaire.getId()),
                webinaire.isActif(),
                webinaire.getDateCreation());
    }

    private InscriptionWebinaireResponse toInscriptionResponse(InscriptionWebinaire inscription) {
        return new InscriptionWebinaireResponse(
                inscription.getId(),
                inscription.getWebinaire().getId(),
                inscription.getWebinaire().getTitre(),
                inscription.getWebinaire().getDateDebut(),
                inscription.isPresenceConfirmee(),
                inscription.getAttestationUrl(),
                inscription.getDateInscription());
    }
}
