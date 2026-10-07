package com.codabli.service;

import com.codabli.dto.LoginRequest;
import com.codabli.dto.LoginResponse;
import com.codabli.dto.RegisterRequest;
import com.codabli.dto.UpdateUtilisateurRequest;
import com.codabli.dto.UtilisateurMapper;
import com.codabli.dto.UtilisateurResponse;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic service for the Utilisateur module.
 * Orchestrates Keycloak operations and local database persistence.
 */
@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final UtilisateurMapper utilisateurMapper;
    private final EntityManager entityManager;
    private final JournalActiviteService journalActiviteService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              KeycloakAdminService keycloakAdminService,
                              UtilisateurMapper utilisateurMapper,
                              EntityManager entityManager,
                              JournalActiviteService journalActiviteService) {
        this.utilisateurRepository = utilisateurRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.utilisateurMapper = utilisateurMapper;
        this.entityManager = entityManager;
        this.journalActiviteService = journalActiviteService;
    }

    /**
     * Registers a new user: creates the user in Keycloak, then persists in the
     * local database.
     */
    public UtilisateurResponse register(RegisterRequest request) {
        // Check if email already exists locally
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        // Validate: eleve must have an ecoleId
        if (request.role() == RoleUtilisateur.eleve && request.ecoleId() == null) {
            throw new IllegalArgumentException("Un élève doit être associé à une école");
        }

        // Create user in Keycloak
        String keycloakId = keycloakAdminService.createUser(
                request.email(),
                request.password(),
                request.prenom(),
                request.nom(),
                request.role().name());

        // Build local entity
        Utilisateur utilisateur = Utilisateur.builder()
                .keycloakId(keycloakId)
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .role(request.role())
                .dateNaissance(request.dateNaissance())
                .languePreferee(request.languePreferee())
                .build();

        // Associate with Ecole if provided
        if (request.ecoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.ecoleId());
            utilisateur.setEcole(ecole);
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toResponse(saved);
    }

    /**
     * Authenticates a user via Keycloak and returns JWT tokens.
     */
    public LoginResponse login(LoginRequest request) {
        return keycloakAdminService.login(request.email(), request.password());
    }

    /**
     * Retrieves the current user's profile by their Keycloak ID (from JWT sub
     * claim).
     */
    @Transactional(readOnly = true)
    public UtilisateurResponse getCurrentUser(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return utilisateurMapper.toResponse(utilisateur);
    }

    Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_KEYCLOAK_USER,
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    Optional<Utilisateur> findByKeycloakId(String id) {
        return utilisateurRepository.findByKeycloakId(id);
    }

    /**
     * Extrait les roles depuis le claim realm_access.roles du JWT.
     * Exemple de claim : { "realm_access": { "roles": ["eleve",
     * "default-roles-codabli"] } }
     */
    @SuppressWarnings("unchecked")
    public Collection<String> extractRoles(Jwt jwt) {
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return java.util.Collections.emptyList();
        }
        return (Collection<String>) realmAccess.get("roles");
    }

    /**
     * Updates the current user's profile.
     */
    public UtilisateurResponse updateProfile(Jwt jwt, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);

        if (request.nom() != null) {
            utilisateur.setNom(request.nom());
        }
        if (request.prenom() != null) {
            utilisateur.setPrenom(request.prenom());
        }
        if (request.dateNaissance() != null) {
            utilisateur.setDateNaissance(request.dateNaissance());
        }
        if (request.languePreferee() != null) {
            utilisateur.setLanguePreferee(request.languePreferee());
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toResponse(saved);
    }

    /**
     * Anonymise un utilisateur au lieu de le supprimer : revoque l'acces
     * Keycloak et efface les donnees personnelles, tout en conservant la
     * ligne locale (integrite referentielle avec les contenus qu'il a pu
     * creer). Action sensible historisee (SUP-03).
     */
    public UtilisateurResponse anonymiserUser(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_USER, "Utilisateur non trouvé avec l'ID: " + id));

        if (utilisateur.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(utilisateur.getKeycloakId());
        }

        utilisateur.setKeycloakId(null);
        utilisateur.setNom("Utilisateur");
        utilisateur.setPrenom("Anonymisé");
        utilisateur.setEmail(null);
        utilisateur.setDateNaissance(null);
        utilisateur.setStatut("inactif");

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        journalActiviteService.enregistrer(jwt, "ANONYMISATION_UTILISATEUR", "Utilisateur " + id,
                "Acces revoque et donnees personnelles effacees");
        return utilisateurMapper.toResponse(saved);
    }

    /**
     * Lists all users (admin operation).
     */
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> getAllUsers() {
        return utilisateurRepository.findAll().stream()
                .map(utilisateurMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a specific user by ID (admin operation).
     */
    @Transactional(readOnly = true)
    public UtilisateurResponse getUserById(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_USER, "Utilisateur non trouvé avec l'ID: " + id));
        return utilisateurMapper.toResponse(utilisateur);
    }

    /**
     * Deletes a user from both Keycloak and the local database (admin operation).
     * Action sensible historisee dans le journal d'activite (SUP-03).
     */
    public void deleteUser(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_USER, "Utilisateur non trouvé avec l'ID: " + id));

        // Delete from Keycloak first
        if (utilisateur.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(utilisateur.getKeycloakId());
        }

        utilisateurRepository.delete(utilisateur);
        journalActiviteService.enregistrer(jwt, "SUPPRESSION_UTILISATEUR",
                "Utilisateur " + id, utilisateur.getPrenom() + " " + utilisateur.getNom() + " (" + utilisateur.getEmail() + ")");
    }

    /**
     * Changes the status of a user (admin operation).
     * Valid statuses: "actif", "inactif", "suspendu"
     * Action sensible historisee dans le journal d'activite (SUP-03).
     */
    public UtilisateurResponse changeUserStatus(UUID id, String newStatus, Jwt jwt) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_USER, "Utilisateur non trouvé avec l'ID: " + id));

        List<String> validStatuses = List.of("actif", "inactif", "suspendu");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Statut invalide: " + newStatus + ". Valeurs acceptées: " + validStatuses);
        }

        String ancienStatut = utilisateur.getStatut();
        utilisateur.setStatut(newStatus);
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        journalActiviteService.enregistrer(jwt, "CHANGEMENT_STATUT_UTILISATEUR",
                "Utilisateur " + id, ancienStatut + " -> " + newStatus);
        return utilisateurMapper.toResponse(saved);
    }

    /**
     * Lists users filtered by role (admin operation).
     */
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> getUsersByRole(RoleUtilisateur role) {
        return utilisateurRepository.findByRole(role).stream()
                .map(utilisateurMapper::toResponse)
                .toList();
    }
}
