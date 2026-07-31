package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contes_danses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 150)
    private String thematique;

    @Column(name = "langue_originale", nullable = false, length = 10)
    private String langueOriginale;

    /** Image de couverture affichee dans le catalogue (CON-01/04). */
    @Column(name = "couverture_url", columnDefinition = "text")
    private String couvertureUrl;

    @Column(length = 100)
    private String pays;

    @Column(length = 150)
    private String culture;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    /** Credits libres : auteur, illustrateur, traducteur, voix, musicien... (RG-05). */
    @Column(columnDefinition = "text")
    private String credits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutConte statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccesConte acces;

    @Column(length = 20)
    private String isbn;

    @Column(name = "fichier_texte_url", columnDefinition = "text")
    private String fichierTexteUrl;

    @Column(name = "fichier_audio_url", columnDefinition = "text")
    private String fichierAudioUrl;

    @Column(name = "fichier_video_url", columnDefinition = "text")
    private String fichierVideoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_enseignant_id")
    private Utilisateur valideParEnseignant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_comite_id")
    private Utilisateur valideParComite;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_publication")
    private OffsetDateTime datePublication;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        if (langueOriginale == null) {
            langueOriginale = "fr";
        }
        if (statut == null) {
            statut = StatutConte.brouillon;
        }
        if (acces == null) {
            acces = AccesConte.gratuit;
        }
    }
}