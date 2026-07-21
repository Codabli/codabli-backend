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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table de liaison entre un élève (Utilisateur) et une Classe.
 *
 * Un élève peut être inscrit dans plusieurs classes, et une classe
 * contient plusieurs élèves. Cette entité matérialise cette relation
 * many-to-many avec des métadonnées (date d'inscription).
 *
 * La contrainte unique (eleve_id, classe_id) empêche un élève
 * d'être inscrit deux fois dans la même classe.
 */
@Entity
@Table(name = "inscriptions_classes", uniqueConstraints = @UniqueConstraint(name = "uk_inscription_eleve_classe", columnNames = {
        "eleve_id", "classe_id" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionClasse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * L'élève inscrit. FetchType.LAZY pour éviter de charger
     * l'arbre complet d'objets à chaque requête.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Utilisateur eleve;

    /**
     * La classe dans laquelle l'élève est inscrit.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @Column(name = "date_inscription", nullable = false)
    private OffsetDateTime dateInscription;

    @PrePersist
    void prePersist() {
        if (dateInscription == null) {
            dateInscription = OffsetDateTime.now();
        }
    }
}
