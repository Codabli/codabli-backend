package com.codabli.service;

import com.codabli.dto.OffreCoachingRequest;
import com.codabli.dto.OffreCoachingResponse;
import com.codabli.dto.ReservationCoachingRequest;
import com.codabli.dto.ReservationCoachingResponse;
import com.codabli.entity.OffreCoaching;
import com.codabli.entity.ReservationCoaching;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutReservationCoaching;
import com.codabli.repository.OffreCoachingRepository;
import com.codabli.repository.ReservationCoachingRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour le coaching (CDC PRO-07).
 */
@Service
@Transactional
public class CoachingService {

    private final OffreCoachingRepository offreCoachingRepository;
    private final ReservationCoachingRepository reservationCoachingRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CoachingService(OffreCoachingRepository offreCoachingRepository,
            ReservationCoachingRepository reservationCoachingRepository,
            UtilisateurRepository utilisateurRepository) {
        this.offreCoachingRepository = offreCoachingRepository;
        this.reservationCoachingRepository = reservationCoachingRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // OFFRES
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OffreCoachingResponse> listerOffres() {
        return offreCoachingRepository.findByActifTrue().stream()
                .map(this::toOffreResponse)
                .collect(Collectors.toList());
    }

    public OffreCoachingResponse creerOffre(OffreCoachingRequest request) {
        OffreCoaching offre = OffreCoaching.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .dureeMinutes(request.getDureeMinutes())
                .tarif(request.getTarif())
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toOffreResponse(offreCoachingRepository.save(offre));
    }

    public OffreCoachingResponse modifierOffre(UUID id, OffreCoachingRequest request) {
        OffreCoaching offre = getOffreOrThrow(id);
        offre.setTitre(request.getTitre());
        offre.setDescription(request.getDescription());
        offre.setDureeMinutes(request.getDureeMinutes());
        offre.setTarif(request.getTarif());
        if (request.getActif() != null) {
            offre.setActif(request.getActif());
        }
        return toOffreResponse(offreCoachingRepository.save(offre));
    }

    public void supprimerOffre(UUID id) {
        offreCoachingRepository.delete(getOffreOrThrow(id));
    }

    // ────────────────────────────────────────────────────────────────
    // RESERVATIONS
    // ────────────────────────────────────────────────────────────────

    public ReservationCoachingResponse reserver(ReservationCoachingRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        OffreCoaching offre = getOffreOrThrow(request.getOffreCoachingId());

        if (!offre.isActif()) {
            throw new IllegalStateException("Cette offre de coaching n'est plus disponible");
        }

        ReservationCoaching reservation = ReservationCoaching.builder()
                .offreCoaching(offre)
                .utilisateur(utilisateur)
                .dateCreneau(request.getDateCreneau())
                .build();

        return toReservationResponse(reservationCoachingRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationCoachingResponse> mesReservations(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return reservationCoachingRepository.findByUtilisateurIdOrderByDateCreneauDesc(utilisateur.getId()).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    public ReservationCoachingResponse changerStatut(UUID reservationId, StatutReservationCoaching statut, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        ReservationCoaching reservation = reservationCoachingRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation de coaching non trouvee avec l'ID: " + reservationId));

        boolean estProprietaire = reservation.getUtilisateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        // Un utilisateur ne peut qu'annuler sa propre reservation ; seul un
        // admin/super_admin peut la confirmer ou la marquer terminee.
        if (statut == StatutReservationCoaching.annulee) {
            if (!estProprietaire && !estAdmin) {
                throw new AccessDeniedException("Vous ne pouvez annuler que vos propres reservations");
            }
        } else if (!estAdmin) {
            throw new AccessDeniedException("Seul un administrateur peut confirmer ou cloturer une reservation");
        }

        reservation.setStatut(statut);
        return toReservationResponse(reservationCoachingRepository.save(reservation));
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private OffreCoaching getOffreOrThrow(UUID id) {
        return offreCoachingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offre de coaching non trouvee avec l'ID: " + id));
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        return utilisateurRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + jwt.getSubject()));
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return java.util.Collections.emptyList();
        }
        return (Collection<String>) realmAccess.get("roles");
    }

    private OffreCoachingResponse toOffreResponse(OffreCoaching offre) {
        return OffreCoachingResponse.builder()
                .id(offre.getId())
                .titre(offre.getTitre())
                .description(offre.getDescription())
                .dureeMinutes(offre.getDureeMinutes())
                .tarif(offre.getTarif())
                .actif(offre.isActif())
                .build();
    }

    private ReservationCoachingResponse toReservationResponse(ReservationCoaching reservation) {
        return ReservationCoachingResponse.builder()
                .id(reservation.getId())
                .offreCoachingId(reservation.getOffreCoaching().getId())
                .offreCoachingTitre(reservation.getOffreCoaching().getTitre())
                .dateCreneau(reservation.getDateCreneau())
                .statut(reservation.getStatut())
                .dateCreation(reservation.getDateCreation())
                .build();
    }
}
