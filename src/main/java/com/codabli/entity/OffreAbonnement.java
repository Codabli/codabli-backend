package com.codabli.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.DureeAbonnement;
import com.codabli.entity.enums.PublicAbonnement;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Offre d'abonnement du catalogue (CDC 7.12/8.14, ABO-01) : famille,
 * enseignant, etablissement ou structure. Geree par l'administration,
 * souscrite par les utilisateurs via Abonnement.
 */
@Entity
@Table(name = "offres_abonnement", uniqueConstraints = @UniqueConstraint(name = "uk_offres_abonnement_code", columnNames = "code"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "public_cible", nullable = false, length = 32)
    private PublicAbonnement publicCible;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DureeAbonnement duree;

    @Column(name = "limite_profils")
    private Integer limiteProfils;

    @Column(name = "limite_classes")
    private Integer limiteClasses;

    @Column(name = "stockage_mo")
    private Integer stockageMo;

    @Column(name = "acces_webinaires", nullable = false)
    @Builder.Default
    private boolean accesWebinaires = false;

    @Column(name = "acces_coaching", nullable = false)
    @Builder.Default
    private boolean accesCoaching = false;

    @Column(name = "acces_exports", nullable = false)
    @Builder.Default
    private boolean accesExports = false;

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
