package com.codabli.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.CategoriePartenaire;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Partenaire de Codabli® (CDC 7.14/8.13, PAR-01/02) : institutionnel,
 * fondation, culturel, association ou citoyen contributeur.
 */
@Entity
@Table(name = "partenaires")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partenaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;

    @Column(columnDefinition = "text")
    private String presentation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CategoriePartenaire categorie;

    @Column(length = 150)
    private String territoire;

    @Column(name = "role_projet", length = 255)
    private String roleProjet;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(columnDefinition = "text")
    private String lien;

    @Column(name = "periode_debut")
    private LocalDate periodeDebut;

    @Column(name = "periode_fin")
    private LocalDate periodeFin;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private OffsetDateTime dateMiseAJour;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        if (dateMiseAJour == null) {
            dateMiseAJour = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = OffsetDateTime.now();
    }
}
