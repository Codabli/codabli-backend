package com.codabli.service;

import com.codabli.dto.CarteAConteRequest;
import com.codabli.dto.CarteAConteResponse;
import com.codabli.entity.CarteAConte;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.CarteAConteRepository;
import com.codabli.repository.InscriptionClasseRepository;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les cartes a conte.
 *
 * Autorisation a deux niveaux :
 * 1. Niveau role (grossier) : gere par @PreAuthorize dans le controller
 * 2. Niveau relation (fin) : gere ICI dans le service, sur la ressource precise
 *
 * Le JWT Keycloak fournit deux informations cles :
 * - jwt.getSubject() → keycloakId de l'utilisateur connecte
 * - realm_access.roles → liste des roles (deja convertis en ROLE_xxx par
 * KeycloakJwtConverter)
 */
@Service
@Transactional
public class CarteAConteService {

    private final CarteAConteRepository carteAConteRepository;
    private final InscriptionClasseRepository inscriptionClasseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityManager entityManager;

    public CarteAConteService(CarteAConteRepository carteAConteRepository,
            InscriptionClasseRepository inscriptionClasseRepository,
            UtilisateurRepository utilisateurRepository,
            EntityManager entityManager) {
        this.carteAConteRepository = carteAConteRepository;
        this.inscriptionClasseRepository = inscriptionClasseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entityManager = entityManager;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — seul un eleve peut creer (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    /**
     * Cree une carte a conte.
     * Le createur est identifie via le JWT, jamais via le body de la requete.
     */
    public CarteAConteResponse creer(CarteAConteRequest request, Jwt jwt) {
        Utilisateur createur = getUtilisateurFromJwt(jwt);

        CarteAConte carte = CarteAConte.builder()
                .createur(createur)
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .texteAssocie(request.getTexteAssocie())
                .build();

        // Rattacher a un conte existant si conteId est fourni
        if (request.getConteId() != null) {
            ConteDanse conte = entityManager.getReference(ConteDanse.class, request.getConteId());
            carte.setConte(conte);
        }

        CarteAConte saved = carteAConteRepository.save(carte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — filtre automatique selon le role du token
    // ────────────────────────────────────────────────────────────────

    /**
     * Retourne les cartes visibles pour l'utilisateur connecte :
     * - admin → toutes les cartes (findAll)
     * - enseignant → cartes des eleves de ses classes (jointure)
     * - eleve → ses propres cartes uniquement
     * - autre → AccessDeniedException
     *
     * On extrait le role depuis les authorities du JWT (deja converties
     * par KeycloakJwtConverter en "ROLE_admin", "ROLE_enseignant", etc.)
     */
    @Transactional(readOnly = true)
    public List<CarteAConteResponse> listerSelonRole(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        List<CarteAConte> cartes;

        if (estModerateurGlobal(roles)) {
            // Admin / super_admin / moderateur : visibilite totale
            cartes = carteAConteRepository.findAll();

        } else if (roles.contains("enseignant")) {
            // Enseignant : uniquement les cartes des eleves de SES classes
            // La requete JPQL traverse : CarteAConte.createur → InscriptionClasse.eleve
            // → InscriptionClasse.classe.enseignant
            cartes = carteAConteRepository.findByEnseignantClasses(utilisateur.getId());

        } else if (roles.contains("eleve")) {
            // Eleve : uniquement ses propres cartes
            cartes = carteAConteRepository.findByCreateurId(utilisateur.getId());

        } else {
            throw new AccessDeniedException("Role non autorise pour acceder aux cartes a conte");
        }

        return cartes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // MODERATION — valider / refuser / demander correction / retirer
    // Verification fine de la relation enseignant-classe-eleve, sauf pour
    // les roles admin / super_admin / moderateur qui ont un droit total
    // (cf. CDC 4.4 "les admins ont un droit total").
    // ────────────────────────────────────────────────────────────────

    /**
     * Valide une carte a conte (MOD-02 : la creation devient publiable).
     */
    public CarteAConteResponse valider(UUID carteId, Jwt jwt) {
        return changerStatut(carteId, jwt, com.codabli.entity.enums.StatutModeration.valide, null);
    }

    /**
     * Refuse une carte a conte (MOD-03 : le motif est enregistre).
     */
    public CarteAConteResponse refuser(UUID carteId, String motif, Jwt jwt) {
        return changerStatut(carteId, jwt, com.codabli.entity.enums.StatutModeration.refuse, motif);
    }

    /**
     * Demande une correction a l'auteur de la carte (MOD-04 : la creation
     * revient au deposant).
     */
    public CarteAConteResponse demanderCorrection(UUID carteId, String motif, Jwt jwt) {
        return changerStatut(carteId, jwt, com.codabli.entity.enums.StatutModeration.a_corriger, motif);
    }

    /**
     * Retire une carte precedemment validee/publiee (MOD-05).
     * Reserve aux roles a droit total (admin/super_admin/moderateur) : un
     * enseignant ne doit pas pouvoir depublier une creation deja validee.
     */
    public CarteAConteResponse retirer(UUID carteId, Jwt jwt) {
        Collection<String> roles = extractRoles(jwt);
        if (!estModerateurGlobal(roles)) {
            throw new AccessDeniedException(
                    "Seuls les administrateurs et moderateurs peuvent retirer une creation publiee");
        }
        return changerStatut(carteId, jwt, com.codabli.entity.enums.StatutModeration.retire, null);
    }

    /**
     * Change le statut de moderation d'une carte, avec verification fine :
     * - admin / super_admin / moderateur → droit total sur toutes les cartes
     * - enseignant → uniquement les cartes des eleves de SES classes
     */
    private CarteAConteResponse changerStatut(UUID carteId, Jwt jwt,
            com.codabli.entity.enums.StatutModeration nouveauStatut, String motif) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        CarteAConte carte = carteAConteRepository.findById(carteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carte a conte non trouvee avec l'ID: " + carteId));

        if (!estModerateurGlobal(roles)) {
            // Enseignant : verification fine, l'eleve createur doit etre dans une
            // classe de cet enseignant
            UUID createurId = carte.getCreateur().getId();
            boolean estDansSaClasse = inscriptionClasseRepository
                    .existsByEleveIdAndClasseEnseignantId(createurId, utilisateur.getId());

            if (!estDansSaClasse) {
                throw new AccessDeniedException(
                        "Vous ne pouvez moderer que les cartes des eleves de vos classes");
            }
        }

        carte.setStatutModeration(nouveauStatut);
        carte.setMotifModeration(motif);
        CarteAConte saved = carteAConteRepository.save(carte);
        return toResponse(saved);
    }

    private boolean estModerateurGlobal(Collection<String> roles) {
        return roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    /**
     * Recupere l'Utilisateur local a partir du keycloakId (claim "sub" du JWT).
     */
    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    /**
     * Extrait les roles depuis le claim realm_access.roles du JWT.
     * Exemple de claim : { "realm_access": { "roles": ["eleve",
     * "default-roles-codabli"] } }
     */
    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return java.util.Collections.emptyList();
        }
        return (Collection<String>) realmAccess.get("roles");
    }

    /**
     * Convertit une entite CarteAConte en DTO de reponse.
     */
    private CarteAConteResponse toResponse(CarteAConte carte) {
        return CarteAConteResponse.builder()
                .id(carte.getId())
                .type(carte.getType())
                .imageUrl(carte.getImageUrl())
                .texteAssocie(carte.getTexteAssocie())
                .statutModeration(carte.getStatutModeration())
                .motifModeration(carte.getMotifModeration())
                .dateCreation(carte.getDateCreation())
                .conteId(carte.getConte() != null ? carte.getConte().getId() : null)
                .createurId(carte.getCreateur().getId())
                .createurNom(carte.getCreateur().getNom())
                .createurPrenom(carte.getCreateur().getPrenom())
                .build();
    }
}
