package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Point d'interet detachable d'une fresque (CDC 8.11, GAL-08/09/12/13).
 * Deux niveaux de lecture : "Je decouvre" (6-8 ans) et "J'en apprends
 * plus" (9-13 ans) — GAL-10.
 */
@Entity
@Table(name = "hotspots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fresque_id", nullable = false)
    private Fresque fresque;

    @Column(nullable = false, length = 255)
    private String titre;

    /** Explication adaptee aux 6-8 ans, "Je decouvre" (GAL-10). */
    @Column(name = "explication_decouverte", columnDefinition = "text")
    private String explicationDecouverte;

    /** Explication approfondie pour les 9-13 ans, "J'en apprends plus" (GAL-10). */
    @Column(name = "explication_approfondie", columnDefinition = "text")
    private String explicationApprofondie;

    /** Element detache de la fresque, forme graphique originale (GAL-09). */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "audio_url", columnDefinition = "text")
    private String audioUrl;

    @Column(name = "ordre_affichage", nullable = false)
    @Builder.Default
    private int ordreAffichage = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = OffsetDateTime.now();
        }
    }
}
