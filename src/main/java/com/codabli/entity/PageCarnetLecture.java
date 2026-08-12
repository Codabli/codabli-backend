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
 * Page de carnet de lecture (CDC 8.17.1, CDL-01 a 17) : espace creatif
 * personnel dans lequel l'enfant conserve une trace de sa lecture d'un
 * Conte Danse. Les champs textuels (resume, mots preferes, reponses) sont
 * ecrits par l'enfant lui-meme (RG-10) — le backend ne fait que structurer
 * et sauvegarder, jamais generer ce contenu.
 */
@Entity
@Table(name = "pages_carnet_lecture")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCarnetLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profil_enfant_id", nullable = false)
    private ProfilEnfant profilEnfant;

    /** Conte lu/ecoute a l'origine de la page (CDL-01), optionnel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conte_id")
    private ConteDanse conte;

    /** Titre/auteur/couverture/langue : preremplis depuis le conte si fourni, modifiables. */
    @Column(length = 255)
    private String titre;

    @Column(length = 255)
    private String auteur;

    @Column(name = "couverture_url", columnDefinition = "text")
    private String couvertureUrl;

    @Column(length = 10)
    private String langue;

    @Column(name = "date_lecture")
    private LocalDate dateLecture;

    @Column(length = 255)
    private String theme;

    /** Resume ecrit par l'enfant avec ses propres mots (CDL-03). */
    @Column(columnDefinition = "text")
    private String resume;

    /** Mots/expressions preferes et leur signification, texte libre (CDL-12). */
    @Column(name = "mots_preferes", columnDefinition = "text")
    private String motsPreferes;

    /** Reponses aux questions personnelles, texte libre (CDL-13). */
    @Column(name = "questions_reponses", columnDefinition = "text")
    private String questionsReponses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutPageCarnet statut;

    /** Lien vers un export PDF/image genere en dehors du backend (CDL-16). */
    @Column(name = "fichier_export_url", columnDefinition = "text")
    private String fichierExportUrl;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordreAffichage ASC")
    @Builder.Default
    private List<ElementCarnetLecture> elements = new ArrayList<>();

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
