package com.codabli.entity;

import com.codabli.entity.enums.TypeRessource;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Représente un matériel ou une fiche pédagogique mis à disposition par le
 * comité de lecture ou les admins.
 */
@Entity
@Table(name = "ressources_pedagogiques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RessourcePedagogique {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeRessource type;

    @Column(name = "fichier_url")
    private String fichierUrl;

    private String thematique;

    @Column(name = "niveau_scolaire")
    private String niveauScolaire;

    @Column(name = "date_ajout", nullable = false)
    private OffsetDateTime dateAjout;

    @Column(nullable = false)
    private boolean actif;

    @PrePersist
    protected void prePersist() {
        if (dateAjout == null) {
            dateAjout = OffsetDateTime.now();
        }
    }
}
