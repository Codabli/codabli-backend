package com.codabli.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.codabli.entity.enums.StatutModeration;
import com.codabli.entity.enums.TypeCarte;

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

@Entity
@Table(name = "cartes_a_conte")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteAConte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conte_id")
    private ConteDanse conte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TypeCarte type;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "texte_associe", columnDefinition = "text")
    private String texteAssocie;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_moderation", nullable = false, length = 32)
    private StatutModeration statutModeration;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = OffsetDateTime.now();
        }
        if (statutModeration == null) {
            statutModeration = StatutModeration.en_attente;
        }
    }
}