package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.StatutAbonnement;

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

/**
 * Souscription d'un utilisateur a une offre d'abonnement (CDC 8.14,
 * ABO-02/03/04).
 */
@Entity
@Table(name = "abonnements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreAbonnement offre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutAbonnement statut;

    @Column(name = "date_debut", nullable = false)
    private OffsetDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private OffsetDateTime dateFin;

    @Column(name = "renouvellement_automatique", nullable = false)
    @Builder.Default
    private boolean renouvellementAutomatique = true;

    /** URL de la facture (document externe, aucune generation PDF cote backend). */
    @Column(name = "facture_url", columnDefinition = "text")
    private String factureUrl;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = OffsetDateTime.now();
        }
        if (statut == null) {
            statut = StatutAbonnement.actif;
        }
    }
}
