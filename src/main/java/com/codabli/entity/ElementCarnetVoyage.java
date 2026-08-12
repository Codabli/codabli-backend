package com.codabli.entity;

import java.util.UUID;

import com.codabli.entity.enums.TypeElementCarnetVoyage;

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
 * Element ajoute par l'enfant a une page de carnet de voyage : element
 * naturel, element culturel, personnage, objet ou creation libre (CDC
 * CDV-04/06/09/10).
 */
@Entity
@Table(name = "elements_carnet_voyage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementCarnetVoyage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private PageCarnetVoyage page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TypeElementCarnetVoyage type;

    @Column(length = 150)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "ordre_affichage", nullable = false)
    @Builder.Default
    private int ordreAffichage = 0;
}
