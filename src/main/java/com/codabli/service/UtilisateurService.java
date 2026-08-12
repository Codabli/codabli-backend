package com.codabli.service;

import com.codabli.dto.*;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        // Validate: eleve must have an ecoleId
        if (request.getRole() == RoleUtilisateur.eleve && request.getEcoleId() == null) {
            throw new IllegalArgumentException("Un élève doit être associé à une école");
        }

        // Create user in Keycloak
        String keycloakId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getPrenom(),
                request.getNom(),
                request.getRole().name());

        // Build local entity
        Utilisateur utilisateur = Utilisateur.builder()
                .keycloakId(keycloakId)
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .role(request.getRole())
                .dateNaissance(request.getDateNaissance())
                .languePreferee(request.getLanguePreferee())
                .build();

        // Associate with Ecole if provided
        if (request.getEcoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.getEcoleId());
            utilisateur.setEcole(ecole);
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toResponse(saved);
    }

    /**
     * Authenticates a user via Keycloak and returns JWT tokens.
     */
    public LoginResponse login(LoginRequest request) {
        return keycloakAdminService.login(request.getEmail(), request.getPassword());
    }

    /**
     * Retrieves the current user's profile by their Keycloak ID (from JWT sub
     * claim).
     */
    @Transactional(readOnly = true)
    public UtilisateurResponse getCurrentUser(String keycloakId) {
        Utilisateur utilisateur = utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Utilisateur non trouvé pour keycloakId: " + keycloakId));
        return utilisateurMapper.toResponse(utilisateur);
    }

    /**
     * Updates the current user's profile.
     */
    public UtilisateurResponse updateProfile(String keycloakId, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (request.getNom() != null) {
            utilisateur.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            utilisateur.setPrenom(request.getPrenom());
        }
        if (request.getDateNaissance() != null) {
            utilisateur.setDateNaissance(request.getDateNaissance());
        }
        if (request.getLanguePreferee() != null) {
            utilisateur.setLanguePreferee(request.getLanguePreferee());
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
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

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
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific user by ID (admin operation).
     */
    @Transactional(readOnly = true)
    public UtilisateurResponse getUserById(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        return utilisateurMapper.toResponse(utilisateur);
    }

    /**
     * Deletes a user from both Keycloak and the local database (admin operation).
     * Action sensible historisee dans le journal d'activite (SUP-03).
     */
    public void deleteUser(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

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
                .collect(Collectors.toList());
    }
}
