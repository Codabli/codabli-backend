package com.codabli.dto;

import com.codabli.entity.Utilisateur;
import org.springframework.stereotype.Component;

/**
 * Maps between Utilisateur entity and DTOs.
 */
@Component
public class UtilisateurMapper {

    public UtilisateurResponse toResponse(Utilisateur utilisateur) {
        return new UtilisateurResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole(),
                utilisateur.getDateNaissance(),
                utilisateur.getLanguePreferee(),
                utilisateur.getStatut(),
                utilisateur.getEcole() != null ? utilisateur.getEcole().getId() : null,
                utilisateur.getEcole() != null ? utilisateur.getEcole().getNom() : null,
                utilisateur.getDateCreation());
    }
}
