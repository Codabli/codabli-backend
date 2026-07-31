package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.CategorieContact;

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
 * Demande envoyée via le formulaire de contact (CDC 8.21, CNT-01/02).
 * Le formulaire est public : "utilisateur" est optionnel, un visiteur non
 * connecté peut envoyer une demande en renseignant directement son nom/email.
 */
@Entity
@Table(name = "demandes_contact")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CategorieContact categorie;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String sujet;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean traite = false;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = OffsetDateTime.now();
        }
    }
}
