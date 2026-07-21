package com.codabli.dto;

import com.codabli.entity.Utilisateur;
import org.springframework.stereotype.Component;

/**
 * Maps between Utilisateur entity and DTOs.
 */
@Component
public class UtilisateurMapper {

    public UtilisateurResponse toResponse(Utilisateur utilisateur) {
        return UtilisateurResponse.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .dateNaissance(utilisateur.getDateNaissance())
                .languePreferee(utilisateur.getLanguePreferee())
                .statut(utilisateur.getStatut())
                .ecoleId(utilisateur.getEcole() != null ? utilisateur.getEcole().getId() : null)
                .ecoleNom(utilisateur.getEcole() != null ? utilisateur.getEcole().getNom() : null)
                .dateCreation(utilisateur.getDateCreation())
                .build();
    }
}
