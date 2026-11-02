package com.codabli.entity;

import com.codabli.dto.parcours.ContenuEtapeParcours;
import com.codabli.dto.parcours.DecouverteParcours;
import com.codabli.dto.parcours.EcritureParcours;
import com.codabli.dto.parcours.MiseEnSceneParcours;
import com.codabli.dto.parcours.RessourcesClasseParcours;
import com.codabli.dto.parcours.RessourcesParcours;
import com.codabli.dto.parcours.TrameParcours;
import com.codabli.dto.parcours.UniversParcours;
import com.codabli.entity.enums.EspaceRepresentation;
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
import java.util.UUID;

/**
 * Travail en cours du parcours "Creer mon conte danse", rattache 1-1 a un
 * conte danse en brouillon.
 * <p>
 * Le contexte (ecran 1) est en colonnes ; les etapes suivantes sont stockees
 * en JSON (une colonne par etape), au format des records de dto.parcours.
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
    private DecouverteParcours decouverte;

    @JdbcTypeCode(SqlTypes.JSON)
    private RessourcesParcours ressources;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_classe")
    private RessourcesClasseParcours ressourcesClasse;

    @JdbcTypeCode(SqlTypes.JSON)
    private UniversParcours univers;

    @JdbcTypeCode(SqlTypes.JSON)
    private TrameParcours trame;

    @JdbcTypeCode(SqlTypes.JSON)
    private EcritureParcours ecriture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mise_en_scene")
    private MiseEnSceneParcours miseEnScene;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    /** Remplace le contenu de l'etape correspondant au type du contenu. */
    public void setEtape(ContenuEtapeParcours contenu) {
        switch (contenu) {
            case DecouverteParcours etape -> decouverte = etape;
            case RessourcesParcours etape -> ressources = etape;
            case RessourcesClasseParcours etape -> ressourcesClasse = etape;
            case UniversParcours etape -> univers = etape;
            case TrameParcours etape -> trame = etape;
            case EcritureParcours etape -> ecriture = etape;
            case MiseEnSceneParcours etape -> miseEnScene = etape;
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
