package com.codabli.service;

import com.codabli.dto.CreationMalletteResponse;
import com.codabli.dto.MalletteArtistesResponse;
import com.codabli.dto.PageCarnetLectureResponse;
import com.codabli.dto.PageCarnetVoyageResponse;
import com.codabli.entity.ProfilEnfant;
import com.codabli.entity.enums.StatutPageCarnet;
import com.codabli.entity.enums.TypeElementCarnetLecture;
import com.codabli.entity.enums.TypeElementCarnetVoyage;
import com.codabli.repository.ProfilEnfantRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vue agregee "Mallette d'Artistes" (CDC 8.16) : rassemble les deux
 * carnets d'un profil enfant et met en avant ses creations et ses pages
 * pretes a imprimer.
 *
 * Reutilise CarnetLectureService/CarnetVoyageService pour la lecture des
 * pages, ce qui garantit la meme verification de propriete (RG-13) sans
 * dupliquer la logique de securite.
 */
@Service
@Transactional(readOnly = true)
public class MalletteArtistesService {

    private final CarnetLectureService carnetLectureService;
    private final CarnetVoyageService carnetVoyageService;
    private final ProfilEnfantRepository profilEnfantRepository;

    public MalletteArtistesService(CarnetLectureService carnetLectureService,
            CarnetVoyageService carnetVoyageService,
            ProfilEnfantRepository profilEnfantRepository) {
        this.carnetLectureService = carnetLectureService;
        this.carnetVoyageService = carnetVoyageService;
        this.profilEnfantRepository = profilEnfantRepository;
    }

    public MalletteArtistesResponse consulter(UUID profilEnfantId, Jwt jwt) {
        List<PageCarnetLectureResponse> carnetLecture = carnetLectureService.listerPages(profilEnfantId, jwt);
        List<PageCarnetVoyageResponse> carnetVoyage = carnetVoyageService.listerPages(profilEnfantId, jwt);

        // Si on arrive ici, la propriete du profil a deja ete verifiee par les
        // deux appels ci-dessus (ils levent une exception sinon).
        ProfilEnfant profil = profilEnfantRepository.findById(profilEnfantId).orElseThrow();

        List<CreationMalletteResponse> creations = new ArrayList<>();
        for (PageCarnetLectureResponse page : carnetLecture) {
            page.getElements().stream()
                    .filter(e -> e.getType() == TypeElementCarnetLecture.creation)
                    .forEach(e -> creations.add(CreationMalletteResponse.builder()
                            .source("carnet_lecture")
                            .pageId(page.getId())
                            .elementId(e.getId())
                            .nom(e.getNom())
                            .imageUrl(e.getImageUrl())
                            .build()));
        }
        for (PageCarnetVoyageResponse page : carnetVoyage) {
            page.getElements().stream()
                    .filter(e -> e.getType() == TypeElementCarnetVoyage.creation)
                    .forEach(e -> creations.add(CreationMalletteResponse.builder()
                            .source("carnet_voyage")
                            .pageId(page.getId())
                            .elementId(e.getId())
                            .nom(e.getNom())
                            .imageUrl(e.getImageUrl())
                            .build()));
        }

        List<PageCarnetLectureResponse> pagesLectureAImprimer = carnetLecture.stream()
                .filter(p -> p.getStatut() == StatutPageCarnet.terminee)
                .collect(Collectors.toList());
        List<PageCarnetVoyageResponse> pagesVoyageAImprimer = carnetVoyage.stream()
                .filter(p -> p.getStatut() == StatutPageCarnet.terminee)
                .collect(Collectors.toList());

        return MalletteArtistesResponse.builder()
                .profilEnfantId(profilEnfantId)
                .pseudonyme(profil.getPseudonyme())
                .carnetLecture(carnetLecture)
                .carnetVoyage(carnetVoyage)
                .creations(creations)
                .pagesLectureAImprimer(pagesLectureAImprimer)
                .pagesVoyageAImprimer(pagesVoyageAImprimer)
                .build();
    }
}
