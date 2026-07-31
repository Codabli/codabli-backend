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
 * Fiche d'activite pedagogique (CDC 8.7, FIC-ACT-01/02), distincte de la
 * RessourcePedagogique generique : structure normee (objectif, materiel,
 * consignes, deroulement, competences, adaptations) pour une seance
 * d'animation autour du Conte Danse®.
 *
 * Memes regles de visibilite que la Mallette Pedagogique (README module 8) :
 * GET reserve enseignant/professionnel_education/admin/comite_lecture,
 * filtre sur actif=true pour les non-gestionnaires ; ecriture reservee
 * admin/comite_lecture ; suppression reservee admin/super_admin.
 */
@Entity
@Table(name = "fiches_activites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FicheActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(columnDefinition = "text")
    private String objectif;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(columnDefinition = "text")
    private String materiel;

    @Column(columnDefinition = "text")
    private String consignes;

    @Column(columnDefinition = "text")
    private String deroulement;

    @Column(columnDefinition = "text")
    private String competences;

    @Column(columnDefinition = "text")
    private String adaptations;

    @Column(columnDefinition = "text")
    private String credits;

    /** FIC-ACT-02 : telechargement au format PDF (document externe). */
    @Column(name = "fichier_pdf_url", columnDefinition = "text")
    private String fichierPdfUrl;

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
