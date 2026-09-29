package com.codabli.service;

import com.codabli.dto.AbonnementRequest;
import com.codabli.dto.AbonnementResponse;
import com.codabli.dto.OffreAbonnementRequest;
import com.codabli.dto.OffreAbonnementResponse;
import com.codabli.entity.Abonnement;
import com.codabli.entity.OffreAbonnement;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;
import com.codabli.entity.enums.StatutAbonnement;
import com.codabli.repository.AbonnementRepository;
import com.codabli.repository.OffreAbonnementRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les abonnements (CDC 8.14, ABO-01 a 04).
 * <p>
 * Aucune passerelle de paiement n'est integree cote backend (comme pour les
 * Commandes de la boutique) : la souscription active directement
 * l'abonnement. Le champ factureUrl reference un document externe, sans
 * generation PDF cote backend.
 */
@Service
@Transactional
public class AbonnementService {

    private final OffreAbonnementRepository offreAbonnementRepository;
    private final AbonnementRepository abonnementRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AbonnementService(OffreAbonnementRepository offreAbonnementRepository,
                             AbonnementRepository abonnementRepository,
                             UtilisateurRepository utilisateurRepository) {
        this.offreAbonnementRepository = offreAbonnementRepository;
        this.abonnementRepository = abonnementRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // OFFRES — catalogue (ABO-01)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OffreAbonnementResponse> listerOffres(PublicAbonnement publicCible) {
        List<OffreAbonnement> offres = publicCible != null
                ? offreAbonnementRepository.findByActifTrueAndPublicCible(publicCible)
                : offreAbonnementRepository.findByActifTrue();
        return offres.stream().map(this::toOffreResponse).toList();
    }

    @Transactional(readOnly = true)
    public OffreAbonnementResponse getOffreById(UUID id) {
        return toOffreResponse(getOffreOrThrow(id));
    }

    public OffreAbonnementResponse creerOffre(OffreAbonnementRequest request) {
        OffreAbonnement offre = OffreAbonnement.builder()
                .code(request.code())
                .nom(request.nom())
                .description(request.description())
                .publicCible(request.publicCible())
                .tarif(request.tarif())
                .duree(request.duree())
                .limiteProfils(request.limiteProfils())
                .limiteClasses(request.limiteClasses())
                .stockageMo(request.stockageMo())
                .accesWebinaires(Boolean.TRUE.equals(request.accesWebinaires()))
                .accesCoaching(Boolean.TRUE.equals(request.accesCoaching()))
                .accesExports(Boolean.TRUE.equals(request.accesExports()))
                .actif(request.actif() == null || request.actif())
                .build();

        OffreAbonnement saved = offreAbonnementRepository.save(offre);
        return toOffreResponse(saved);
    }

    public OffreAbonnementResponse modifierOffre(UUID id, OffreAbonnementRequest request) {
        OffreAbonnement offre = getOffreOrThrow(id);

        offre.setCode(request.code());
        offre.setNom(request.nom());
        offre.setDescription(request.description());
        offre.setPublicCible(request.publicCible());
        offre.setTarif(request.tarif());
        offre.setDuree(request.duree());
        offre.setLimiteProfils(request.limiteProfils());
        offre.setLimiteClasses(request.limiteClasses());
        offre.setStockageMo(request.stockageMo());
        if (request.accesWebinaires() != null) {
            offre.setAccesWebinaires(request.accesWebinaires());
        }
        if (request.accesCoaching() != null) {
            offre.setAccesCoaching(request.accesCoaching());
        }
        if (request.accesExports() != null) {
            offre.setAccesExports(request.accesExports());
        }
        if (request.actif() != null) {
            offre.setActif(request.actif());
        }

        OffreAbonnement saved = offreAbonnementRepository.save(offre);
        return toOffreResponse(saved);
    }

    public void supprimerOffre(UUID id) {
        OffreAbonnement offre = getOffreOrThrow(id);
        offreAbonnementRepository.delete(offre);
    }

    // ────────────────────────────────────────────────────────────────
    // SOUSCRIPTION — ABO-02
    // ────────────────────────────────────────────────────────────────

    public AbonnementResponse souscrire(AbonnementRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        OffreAbonnement offre = getOffreOrThrow(request.offreId());

        if (!offre.isActif()) {
            throw new IllegalStateException("Cette offre n'est plus disponible a la souscription");
        }

        OffsetDateTime debut = OffsetDateTime.now();
        Abonnement abonnement = Abonnement.builder()
                .utilisateur(utilisateur)
                .offre(offre)
                .statut(StatutAbonnement.actif)
                .dateDebut(debut)
                .dateFin(ajouterDuree(debut, offre.getDuree()))
                .renouvellementAutomatique(true)
                .build();

        Abonnement saved = abonnementRepository.save(abonnement);
        return toAbonnementResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MES ABONNEMENTS — ABO-03/04 (expiration + factures)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AbonnementResponse> mesAbonnements(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return abonnementRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateur.getId())
                .stream()
                .map(this::toAbonnementResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — ABO-03 (renouveler / resilier / changer d'offre)
    // ────────────────────────────────────────────────────────────────

    public AbonnementResponse renouveler(UUID id, Jwt jwt) {
        Abonnement abonnement = getAbonnementAvecDroit(id, jwt);

        OffsetDateTime base = abonnement.getDateFin().isAfter(OffsetDateTime.now())
                ? abonnement.getDateFin()
                : OffsetDateTime.now();
        abonnement.setDateFin(ajouterDuree(base, abonnement.getOffre().getDuree()));
        abonnement.setStatut(StatutAbonnement.actif);

        Abonnement saved = abonnementRepository.save(abonnement);
        return toAbonnementResponse(saved);
    }

    public AbonnementResponse resilier(UUID id, Jwt jwt) {
        Abonnement abonnement = getAbonnementAvecDroit(id, jwt);
        abonnement.setStatut(StatutAbonnement.resilie);
        abonnement.setRenouvellementAutomatique(false);

        Abonnement saved = abonnementRepository.save(abonnement);
        return toAbonnementResponse(saved);
    }

    public AbonnementResponse changerOffre(UUID id, AbonnementRequest request, Jwt jwt) {
        Abonnement abonnement = getAbonnementAvecDroit(id, jwt);
        OffreAbonnement nouvelleOffre = getOffreOrThrow(request.offreId());

        if (!nouvelleOffre.isActif()) {
            throw new IllegalStateException("Cette offre n'est plus disponible a la souscription");
        }

        OffsetDateTime debut = OffsetDateTime.now();
        abonnement.setOffre(nouvelleOffre);
        abonnement.setDateDebut(debut);
        abonnement.setDateFin(ajouterDuree(debut, nouvelleOffre.getDuree()));
        abonnement.setStatut(StatutAbonnement.actif);

        Abonnement saved = abonnementRepository.save(abonnement);
        return toAbonnementResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    /**
     * Recupere un abonnement en verifiant que l'appelant en est le
     * proprietaire, ou dispose d'un role admin/super_admin.
     */
    private Abonnement getAbonnementAvecDroit(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Abonnement non trouve avec l'ID: " + id));

        boolean estProprietaire = abonnement.getUtilisateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        if (!estProprietaire && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez gerer que vos propres abonnements");
        }

        return abonnement;
    }

    private OffsetDateTime ajouterDuree(OffsetDateTime base, DureeAbonnement duree) {
        return duree == DureeAbonnement.annuel ? base.plusYears(1) : base.plusMonths(1);
    }

    private OffreAbonnement getOffreOrThrow(UUID id) {
        return offreAbonnementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offre d'abonnement non trouvee avec l'ID: " + id));
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

    private OffreAbonnementResponse toOffreResponse(OffreAbonnement offre) {
        return new OffreAbonnementResponse(
                offre.getId(),
                offre.getCode(),
                offre.getNom(),
                offre.getDescription(),
                offre.getPublicCible(),
                offre.getTarif(),
                offre.getDuree(),
                offre.getLimiteProfils(),
                offre.getLimiteClasses(),
                offre.getStockageMo(),
                offre.isAccesWebinaires(),
                offre.isAccesCoaching(),
                offre.isAccesExports(),
                offre.isActif());
    }

    /**
     * Convertit un Abonnement en DTO. Le statut affiche est recalcule a la
     * volee (sans persistance) si la date de fin est depassee, en l'absence
     * de tache planifiee dans ce backend.
     */
    private AbonnementResponse toAbonnementResponse(Abonnement abonnement) {
        StatutAbonnement statutAffiche = abonnement.getStatut();
        if (statutAffiche == StatutAbonnement.actif && abonnement.getDateFin().isBefore(OffsetDateTime.now())) {
            statutAffiche = StatutAbonnement.expire;
        }

        return new AbonnementResponse(abonnement.getId(),
                abonnement.getOffre().getId(),
                abonnement.getOffre().getNom(),
                abonnement.getOffre().getCode(),
                statutAffiche,
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.isRenouvellementAutomatique(),
                abonnement.getFactureUrl(),
                abonnement.getDateCreation());


    }
}
