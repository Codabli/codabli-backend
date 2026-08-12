package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Statistiques agregees pour le tableau de bord administrateur (CDC
 * section 15). Ne trace aucune donnee individuelle sur un enfant — ces
 * chiffres servent a ameliorer l'experience pedagogique, pas a profiler
 * un utilisateur (RG "Protection des donnees").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesResponse {

    private long totalUtilisateurs;
    private Map<String, Long> utilisateursParRole;

    private long totalEcoles;
    private long totalClasses;

    private long totalContesDanses;
    private Map<String, Long> contesDansesParStatut;

    private long totalCartesAConte;
    private Map<String, Long> cartesAConteParStatut;

    private long totalRessourcesPedagogiques;
    private long totalFichesActivites;

    private long totalProduits;
    private long totalCommandes;
    private Map<String, Long> commandesParStatut;

    private long totalPartenaires;
    private long totalOffresAbonnement;
    private Map<String, Long> abonnementsParStatut;

    private long totalProfilsEnfants;

    private long totalPagesCarnetLecture;
    private long pagesCarnetLectureTerminees;
    private long totalPagesCarnetVoyage;
    private long pagesCarnetVoyageTerminees;

    private long totalActualitesPubliees;

    private long totalDemandesContact;
    private long demandesContactNonTraitees;
}
