package com.codabli.service;

import com.codabli.dto.*;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
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

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
            KeycloakAdminService keycloakAdminService,
            UtilisateurMapper utilisateurMapper,
            EntityManager entityManager) {
        this.utilisateurRepository = utilisateurRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.utilisateurMapper = utilisateurMapper;
        this.entityManager = entityManager;
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
     */
    public void deleteUser(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        // Delete from Keycloak first
        if (utilisateur.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(utilisateur.getKeycloakId());
        }

        utilisateurRepository.delete(utilisateur);
    }

    /**
     * Changes the status of a user (admin operation).
     * Valid statuses: "actif", "inactif", "suspendu"
     */
    public UtilisateurResponse changeUserStatus(UUID id, String newStatus) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        List<String> validStatuses = List.of("actif", "inactif", "suspendu");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Statut invalide: " + newStatus + ". Valeurs acceptées: " + validStatuses);
        }

        utilisateur.setStatut(newStatus);
        Utilisateur saved = utilisateurRepository.save(utilisateur);
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
