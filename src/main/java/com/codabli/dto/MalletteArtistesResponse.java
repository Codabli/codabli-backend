package com.codabli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Vue agregee de l'espace creatif d'un profil enfant (CDC 8.16, "Ma
 * Mallette d'artistes") : ses deux carnets, ses creations, et les pages
 * pretes a etre imprimees/exportees.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MalletteArtistesResponse {

    private UUID profilEnfantId;

    private String pseudonyme;

    private List<PageCarnetLectureResponse> carnetLecture;

    private List<PageCarnetVoyageResponse> carnetVoyage;

    /** Dessins/collages/imports retrouves dans les deux carnets (CDL-09/11, CDV-09). */
    private List<CreationMalletteResponse> creations;

    /** Pages terminees, pretes a etre imprimees (CDL-16). */
    private List<PageCarnetLectureResponse> pagesLectureAImprimer;

    private List<PageCarnetVoyageResponse> pagesVoyageAImprimer;
}
