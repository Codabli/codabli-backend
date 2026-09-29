package com.codabli.dto;

import java.util.Map;

/**
 * Statistiques agregees pour le tableau de bord administrateur (CDC
 * section 15). Ne trace aucune donnee individuelle sur un enfant — ces
 * chiffres servent a ameliorer l'experience pedagogique, pas a profiler
 * un utilisateur (RG "Protection des donnees").
 */
public record StatistiquesResponse(
        long totalUtilisateurs,
        Map<String, Long> utilisateursParRole,
        long totalEcoles,
        long totalClasses,
        long totalContesDanses,
        Map<String, Long> contesDansesParStatut,
        long totalCartesAConte,
        Map<String, Long> cartesAConteParStatut,
        long totalRessourcesPedagogiques,
        long totalFichesActivites,
        long totalProduits,
        long totalCommandes,
        Map<String, Long> commandesParStatut,
        long totalPartenaires,
        long totalOffresAbonnement,
        Map<String, Long> abonnementsParStatut,
        long totalProfilsEnfants,
        long totalPagesCarnetLecture,
        long pagesCarnetLectureTerminees,
        long totalPagesCarnetVoyage,
        long pagesCarnetVoyageTerminees,
        long totalActualitesPubliees,
        long totalDemandesContact,
        long demandesContactNonTraitees) {
}
