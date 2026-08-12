package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Salle culturelle de la Galerie des Arts (CDC 8.11, GAL-01/03) : un pays
 * accessible depuis le hall, avec son ambiance et sa fresque principale.
 */
@Entity
@Table(name = "salles_galerie")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleGalerie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String pays;

    @Column(columnDefinition = "text")
    private String introduction;

    @Column(name = "ambiance_sonore_url", columnDefinition = "text")
    private String ambianceSonoreUrl;

    @Column(name = "palette_couleurs", length = 255)
    private String paletteCouleurs;

    @Column(name = "ordre_affichage", nullable = false)
    @Builder.Default
    private int ordreAffichage = 0;

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
