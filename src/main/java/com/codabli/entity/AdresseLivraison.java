package com.codabli.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "adresses_livraison")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(nullable = false, columnDefinition = "text")
    private String adresse;

    @Column(name = "code_postal", nullable = false, length = 10)
    private String codePostal;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(length = 20)
    private String telephone;

    @Column(name = "par_defaut", nullable = false)
    @Builder.Default
    private boolean parDefaut = false;
}
