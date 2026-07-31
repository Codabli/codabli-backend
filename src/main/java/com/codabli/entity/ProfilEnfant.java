package com.codabli.entity;

import java.time.LocalDate;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profil enfant leger rattache au compte d'un adulte responsable (parent ou
 * professionnel de l'education) — CDC 4.1/4.2/8.16/8.17, FAM-02, ENF-01.
 *
 * A la difference du role "eleve" (compte Keycloak complet lie a une ecole
 * pour le workflow pedagogique de classe/moderation), ce profil n'a pas de
 * compte de connexion propre : il est gere depuis le compte du responsable,
 * ce qui garantit nativement RG-03 (l'enfant n'a jamais acces aux donnees du
 * parent, aux parametres du compte adulte ni aux fonctionnalites de
 * paiement, puisqu'il n'a pas de session independante dans ce backend).
 */
@Entity
@Table(name = "profils_enfants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilEnfant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Compte adulte proprietaire du profil (role parent ou professionnel_education). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Utilisateur responsable;

    /** RG-03 : pseudonyme privilegie, jamais le nom complet en public. */
    @Column(nullable = false, length = 100)
    private String pseudonyme;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "langue_preferee", length = 10)
    private String languePreferee;

    /** Preferences libres (themes, formats, etc.), hors gamification. */
    @Column(columnDefinition = "text")
    private String preferences;

    /** Besoins d'accessibilite specifiques (CDC 4.3 "Normes dans le cadre du developpement"). */
    @Column(columnDefinition = "text")
    private String accessibilite;

    /** RG-04 : autorisation enregistree, datee, consultable et revocable. */
    @Column(name = "autorisation_parentale", nullable = false)
    @Builder.Default
    private boolean autorisationParentale = false;

    @Column(name = "date_autorisation")
    private OffsetDateTime dateAutorisation;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

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
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = OffsetDateTime.now();
    }
}
