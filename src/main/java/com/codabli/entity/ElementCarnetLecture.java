package com.codabli.entity;

import java.util.UUID;

import com.codabli.entity.enums.TypeElementCarnetLecture;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Element ajoute par l'enfant a une page de carnet de lecture : personnage,
 * lieu, objet magique ou creation libre (dessin/collage/import) — CDC
 * CDL-04/05/06/07/09/11. Un seul type generique couvre ces sous-listes du
 * CDC (nom + description libre + image), pour rester simple sans perdre
 * l'intention fonctionnelle.
 */
@Entity
@Table(name = "elements_carnet_lecture")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private PageCarnetLecture page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TypeElementCarnetLecture type;

    @Column(length = 150)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    /** Dessin de l'enfant, autocollant utilise, ou creation importee (CDL-04/09/11). */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "ordre_affichage", nullable = false)
    @Builder.Default
    private int ordreAffichage = 0;
}
