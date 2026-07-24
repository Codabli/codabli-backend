package com.codabli.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Représente une mise en avant éditoriale d'une CarteAConte dans la Galerie
 * d'art.
 * Permet aux admins de mettre en valeur certaines œuvres (ex: "œuvre du mois").
 */
@Entity
@Table(name = "galerie_mises_en_avant", uniqueConstraints = @UniqueConstraint(name = "uk_galerie_carte_a_conte", columnNames = "carte_a_conte_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalerieMiseEnAvant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carte_a_conte_id", nullable = false)
    private CarteAConte carteAConte;

    @Column(name = "date_debut", nullable = false)
    private OffsetDateTime dateDebut;

    @Column(name = "date_fin")
    private OffsetDateTime dateFin;

    @Column(name = "ordre_affichage", nullable = false)
    private Integer ordreAffichage;

    @Column(nullable = false)
    private boolean actif;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = OffsetDateTime.now();
        }
        if (dateDebut == null) {
            dateDebut = OffsetDateTime.now();
        }
    }
}
