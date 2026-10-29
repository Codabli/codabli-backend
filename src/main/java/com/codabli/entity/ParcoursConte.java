package com.codabli.entity;

import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.entity.enums.EtapeParcours;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Travail en cours du parcours "Creer mon conte danse", rattache 1-1 a un
 * conte danse en brouillon.
 * <p>
 * Le contexte (ecran 1) est en colonnes ; les etapes suivantes sont stockees
 * en JSON, tel que le front les manipule (une colonne par etape).
 */
@Entity
@Table(name = "parcours_conte")
@Getter
@Setter
@NoArgsConstructor
public class ParcoursConte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conte_danse_id", nullable = false, unique = true)
    private UUID conteDanseId;

    // ── Contexte (ecran 1) ──

    @Column(length = 150)
    private String region;

    @Column(length = 150)
    private String ville;

    @Enumerated(EnumType.STRING)
    @Column(name = "espace_representation", length = 32)
    private EspaceRepresentation espaceRepresentation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ingredients_secrets", nullable = false)
    private List<String> ingredientsSecrets = new ArrayList<>();

    // ── Etapes suivantes (JSON) ──

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> decouverte;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> ressources;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_classe")
    private Map<String, Object> ressourcesClasse;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> univers;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> trame;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> ecriture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mise_en_scene")
    private Map<String, Object> miseEnScene;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    public Map<String, Object> getEtape(EtapeParcours etape) {
        return switch (etape) {
            case decouverte -> decouverte;
            case ressources -> ressources;
            case ressources_classe -> ressourcesClasse;
            case univers -> univers;
            case trame -> trame;
            case ecriture -> ecriture;
            case mise_en_scene -> miseEnScene;
        };
    }

    public void setEtape(EtapeParcours etape, Map<String, Object> contenu) {
        switch (etape) {
            case decouverte -> decouverte = contenu;
            case ressources -> ressources = contenu;
            case ressources_classe -> ressourcesClasse = contenu;
            case univers -> univers = contenu;
            case trame -> trame = contenu;
            case ecriture -> ecriture = contenu;
            case mise_en_scene -> miseEnScene = contenu;
        }
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        dateModification = now;
    }

    @PreUpdate
    void preUpdate() {
        dateModification = OffsetDateTime.now();
    }
}
