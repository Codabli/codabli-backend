package com.codabli.dto;

import java.util.List;
import java.util.UUID;

/**
 * Vue agregee de l'espace creatif d'un profil enfant (CDC 8.16, "Ma
 * Mallette d'artistes") : ses deux carnets, ses creations, et les pages
 * pretes a etre imprimees/exportees.
 */
public record MalletteArtistesResponse(
        UUID profilEnfantId,
        String pseudonyme,
        List<PageCarnetLectureResponse> carnetLecture,
        List<PageCarnetVoyageResponse> carnetVoyage,
        // Dessins/collages/imports retrouves dans les deux carnets (CDL-09/11, CDV-09).
        List<CreationMalletteResponse> creations,
        // Pages terminees, pretes a etre imprimees (CDL-16).
        List<PageCarnetLectureResponse> pagesLectureAImprimer,
        List<PageCarnetVoyageResponse> pagesVoyageAImprimer) {
}
