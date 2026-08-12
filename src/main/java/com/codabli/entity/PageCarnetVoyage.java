package com.codabli.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.codabli.entity.enums.StatutPageCarnet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
 * Page de carnet de voyage (CDC 8.17.2, CDV-01 a 14) : espace creatif
 * personnel associe a un pays/une fresque decouverts par l'enfant. Comme
 * pour le carnet de lecture, les champs de texte libre sont ecrits par
 * l'enfant (RG-10) — seules les metadonnees objectives (nom du pays,
 * fresque associee) peuvent etre preremplies.
 */
@Entity
@Table(name = "pages_carnet_voyage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetVoyage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profil_enfant_id", nullable = false)
    private ProfilEnfant profilEnfant;

    /** Conte Danse / fresque associe au pays decouvert (CDV-01), optionnel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conte_id")
    private ConteDanse conte;

    @Column(length = 100)
    private String pays;

    @Column(name = "drapeau_url", columnDefinition = "text")
    private String drapeauUrl;

    @Column(name = "langues_decouvertes", length = 255)
    private String languesDecouvertes;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    /** CDV-03 : identite du pays — ce qui etait connu, decouvert, question, mot associe. */
    @Column(name = "identite_notes", columnDefinition = "text")
    private String identiteNotes;

    /** CDV-04 : nature et cadre de vie. */
    @Column(name = "nature_notes", columnDefinition = "text")
    private String natureNotes;

    /** CDV-05 : societe. */
    @Column(name = "societe_notes", columnDefinition = "text")
    private String societeNotes;

    /** CDV-06 : culture. */
    @Column(name = "culture_notes", columnDefinition = "text")
    private String cultureNotes;

    /** CDV-08 : description de l'experience personnelle. */
    @Column(name = "experience_notes", columnDefinition = "text")
    private String experienceNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutPageCarnet statut;

    @Column(name = "fichier_export_url", columnDefinition = "text")
    private String fichierExportUrl;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordreAffichage ASC")
    @Builder.Default
    private List<ElementCarnetVoyage> elements = new ArrayList<>();

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
        if (statut == null) {
            statut = StatutPageCarnet.non_commencee;
        }
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = OffsetDateTime.now();
    }
}
