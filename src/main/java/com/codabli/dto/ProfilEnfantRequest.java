package com.codabli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO d'entree pour la creation/modification d'un profil enfant (FAM-02).
 * Le responsable n'est PAS dans le DTO — il est extrait du JWT dans le
 * service (principe de securite, identique a Actualite/CarteAConte).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilEnfantRequest {

    @NotBlank(message = "Le pseudonyme est obligatoire")
    @Size(max = 100)
    private String pseudonyme;

    private LocalDate dateNaissance;

    private String languePreferee;

    private String preferences;

    private String accessibilite;

    private Boolean autorisationParentale;
}
