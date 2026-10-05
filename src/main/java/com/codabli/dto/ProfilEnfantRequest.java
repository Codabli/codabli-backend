package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO d'entree pour la creation/modification d'un profil enfant (FAM-02).
 * Le responsable n'est PAS dans le DTO — il est extrait du JWT dans le
 * service (principe de securite, identique a Actualite/CarteAConte).
 */
public record ProfilEnfantRequest(
        @NotBlank(message = "Le pseudonyme est obligatoire")
        @Size(max = 100)
        String pseudonyme,
        LocalDate dateNaissance,
        String languePreferee,
        String preferences,
        String accessibilite,
        Boolean autorisationParentale) {
}
