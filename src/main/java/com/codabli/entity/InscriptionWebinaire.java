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
 * Inscription d'un utilisateur a un webinaire (CDC PRO-06).
 */
@Entity
@Table(name = "inscriptions_webinaire", uniqueConstraints = @UniqueConstraint(name = "uk_inscription_webinaire_utilisateur", columnNames = {
        "webinaire_id", "utilisateur_id" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionWebinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webinaire_id", nullable = false)
    private Webinaire webinaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "presence_confirmee", nullable = false)
    @Builder.Default
    private boolean presenceConfirmee = false;

    /** Lien vers l'attestation de presence, si generee (document externe). */
    @Column(name = "attestation_url", columnDefinition = "text")
    private String attestationUrl;

    @Column(name = "date_inscription", nullable = false)
    private OffsetDateTime dateInscription;

    @PrePersist
    void prePersist() {
        if (dateInscription == null) {
            dateInscription = OffsetDateTime.now();
        }
    }
}
