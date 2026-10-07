package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.codabli.entity.enums.EspaceRepresentation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Projet de conte danse d'un enseignant (parcours "Creer mon conte", epic SCRUM-29).
 * <p>
 * L'ecran 1 renseigne le contexte du projet ; les ecrans suivants enrichiront ce projet.
 */
@Entity
@Table(name = "projets_contes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjetConte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Utilisateur enseignant;

    // ── Contexte du projet (ecran 1) ──

    @Column(name = "tranche_age", nullable = false, length = 5)
    private String trancheAge;

    @Column(nullable = false, length = 100)
    private String pays;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(length = 100)
    private String ville;

    @Column(nullable = false, length = 150)
    private String theme;

    @ElementCollection
    @CollectionTable(name = "projets_contes_ingredients",
            joinColumns = @JoinColumn(name = "projet_conte_id"))
    @OrderColumn(name = "position")
    @Column(name = "ingredient", nullable = false, length = 40)
    @Builder.Default
    private List<String> ingredientsSecrets = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "espace_representation", length = 20)
    private EspaceRepresentation espaceRepresentation;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        dateModification = now;
    }

    @PreUpdate
    void preUpdate() {
        dateModification = OffsetDateTime.now();
    }
}
