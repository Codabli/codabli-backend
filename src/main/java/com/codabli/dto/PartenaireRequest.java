package com.codabli.dto;

import com.codabli.entity.enums.CategoriePartenaire;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO d'entree pour la creation/modification d'un partenaire (PAR-01/02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartenaireRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255)
    private String nom;

    private String logoUrl;

    private String presentation;

    @NotNull(message = "La categorie est obligatoire")
    private CategoriePartenaire categorie;

    private String territoire;

    private String roleProjet;

    private String videoUrl;

    private String lien;

    private LocalDate periodeDebut;

    private LocalDate periodeFin;

    private Boolean actif;
}
