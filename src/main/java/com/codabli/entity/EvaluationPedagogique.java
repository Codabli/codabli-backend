package com.codabli.entity;

import java.time.LocalDate;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evaluation pedagogique (CDC PRO-05) : grille d'observation, competences,
 * bilan pour une classe et/ou un eleve, redigee par l'enseignant.
 */
@Entity
@Table(name = "evaluations_pedagogiques")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationPedagogique {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Utilisateur enseignant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleve_id")
    private Utilisateur eleve;

    @Column(nullable = false, length = 255)
    private String titre;

    /** Grille de competences evaluees, texte libre (PRO-05). */
    @Column(name = "competences_evaluees", columnDefinition = "text")
    private String competencesEvaluees;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(columnDefinition = "text")
    private String bilan;

    @Column(name = "date_evaluation")
    private LocalDate dateEvaluation;

    /** Lien vers un export du bilan, document externe (PRO-05 "Export"). */
    @Column(name = "fichier_export_url", columnDefinition = "text")
    private String fichierExportUrl;

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
        if (dateEvaluation == null) {
            dateEvaluation = LocalDate.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = OffsetDateTime.now();
    }
}
