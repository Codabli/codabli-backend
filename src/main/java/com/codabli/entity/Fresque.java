package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fresque culturelle principale d'une salle (CDC 8.11, GAL-06/07).
 */
@Entity
@Table(name = "fresques")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fresque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false, unique = true)
    private SalleGalerie salle;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String introduction;

    /** Conte Danse associe (GAL-16, lien avec le carnet de voyage). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conte_id")
    private ConteDanse conte;

    @OneToMany(mappedBy = "fresque", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordreAffichage ASC")
    @Builder.Default
    private List<Hotspot> hotspots = new ArrayList<>();

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
