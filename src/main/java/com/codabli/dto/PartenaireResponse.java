package com.codabli.dto;

import com.codabli.entity.enums.CategoriePartenaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour un partenaire (PAR-02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartenaireResponse {

    private UUID id;

    private String nom;

    private String logoUrl;

    private String presentation;

    private CategoriePartenaire categorie;

    private String territoire;

    private String roleProjet;

    private String videoUrl;

    private String lien;

    private LocalDate periodeDebut;

    private LocalDate periodeFin;

    private boolean actif;

    private OffsetDateTime dateCreation;

    private OffsetDateTime dateMiseAJour;
}
