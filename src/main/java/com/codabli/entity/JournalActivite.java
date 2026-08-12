package com.codabli.entity;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Journal des actions sensibles (CDC 8.23, SUP-03) : suppression/suspension
 * d'utilisateur, retrait d'une creation publiee, etc. Consultation reservee
 * au super-administrateur.
 */
@Entity
@Table(name = "journal_activite")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Auteur de l'action. Nullable : une action systeme n'a pas d'acteur humain. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acteur_id")
    private Utilisateur acteur;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 255)
    private String cible;

    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "date_action", nullable = false)
    private OffsetDateTime dateAction;

    @PrePersist
    void prePersist() {
        if (dateAction == null) {
            dateAction = OffsetDateTime.now();
        }
    }
}
