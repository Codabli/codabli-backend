package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.TypeCibleMiseEnAvant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mise en avant generique sur la page d'accueil (CDC ACC-04) : une
 * actualite, un conte, un produit, un abonnement, un partenaire ou un
 * evenement, pour une duree definie par l'administrateur.
 *
 * Reference la cible par type + ID plutot que par une FK dediee par type,
 * pour couvrir plusieurs entites sans dupliquer le mecanisme (a la
 * difference de GalerieMiseEnAvant, specifique aux Cartes a Conte de la
 * galerie).
 */
@Entity
@Table(name = "mises_en_avant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiseEnAvant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_cible", nullable = false, length = 16)
    private TypeCibleMiseEnAvant typeCible;

    @Column(name = "cible_id", nullable = false)
    private UUID cibleId;

    @Column(length = 255)
    private String titre;

    @Column(name = "date_debut", nullable = false)
    private OffsetDateTime dateDebut;

    @Column(name = "date_fin")
    private OffsetDateTime dateFin;

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
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        if (dateDebut == null) {
            dateDebut = now;
        }
    }
}
