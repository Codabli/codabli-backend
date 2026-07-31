package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.StatutTraduction;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Version linguistique d'un Conte Danse (CDC 8.5, LAN-01/02/03). Un conte
 * peut avoir plusieurs traductions, chacune suivant son propre cycle de
 * traduction/relecture/publication.
 */
@Entity
@Table(name = "contes_danses_traductions", uniqueConstraints = @UniqueConstraint(name = "uk_traduction_conte_langue", columnNames = {
        "conte_id", "langue" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteDanseTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conte_id", nullable = false)
    private ConteDanse conte;

    @Column(nullable = false, length = 10)
    private String langue;

    @Column(length = 50)
    private String variante;

    @Column(columnDefinition = "text")
    private String texte;

    @Column(name = "audio_url", columnDefinition = "text")
    private String audioUrl;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "sous_titres_url", columnDefinition = "text")
    private String sousTitresUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traducteur_id")
    private Utilisateur traducteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relecteur_id")
    private Utilisateur relecteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutTraduction statut;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private OffsetDateTime dateMiseAJour;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        if (dateMiseAJour == null) {
            dateMiseAJour = now;
        }
        if (statut == null) {
            statut = StatutTraduction.a_traduire;
        }
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = OffsetDateTime.now();
    }
}
