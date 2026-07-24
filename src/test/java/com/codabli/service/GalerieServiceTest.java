package com.codabli.service;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.dto.GalerieMiseEnAvantRequest;
import com.codabli.dto.GalerieMiseEnAvantResponse;
import com.codabli.entity.CarteAConte;
import com.codabli.entity.GalerieMiseEnAvant;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutModeration;
import com.codabli.entity.enums.TypeCarte;
import com.codabli.repository.CarteAConteRepository;
import com.codabli.repository.GalerieMiseEnAvantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour GalerieService.
 */
@ExtendWith(MockitoExtension.class)
class GalerieServiceTest {

    @Mock
    private GalerieMiseEnAvantRepository galerieMiseEnAvantRepository;

    @Mock
    private CarteAConteRepository carteAConteRepository;

    @InjectMocks
    private GalerieService galerieService;

    private CarteAConte carteValide;
    private Utilisateur createur;

    @BeforeEach
    void setUp() {
        createur = new Utilisateur();
        createur.setId(UUID.randomUUID());
        createur.setPrenom("Amine");
        createur.setNom("Test");

        carteValide = new CarteAConte();
        carteValide.setId(UUID.randomUUID());
        carteValide.setType(TypeCarte.personnage);
        carteValide.setImageUrl("https://example.com/image.jpg");
        carteValide.setTexteAssocie("Le petit prince");
        carteValide.setStatutModeration(StatutModeration.valide);
        carteValide.setDateCreation(OffsetDateTime.now());
        carteValide.setCreateur(createur);
    }

    @Test
    void lister_shouldReturnOnlyValidCards() {
        Pageable pageable = PageRequest.of(0, 10);
        GalerieItemResponse item = GalerieItemResponse.builder()
                .id(carteValide.getId())
                .type(TypeCarte.personnage)
                .imageUrl("https://example.com/image.jpg")
                .texteAssocie("Le petit prince")
                .createurPrenom("Amine")
                .miseEnAvant(false)
                .build();

        Page<GalerieItemResponse> page = new PageImpl<>(List.of(item), pageable, 1);
        when(galerieMiseEnAvantRepository.findCartesForGalerie(
                eq(StatutModeration.valide), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(page);

        Page<GalerieItemResponse> result = galerieService.listerGalerie(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Le petit prince", result.getContent().get(0).getTexteAssocie());
        verify(galerieMiseEnAvantRepository).findCartesForGalerie(
                eq(StatutModeration.valide), any(OffsetDateTime.class), eq(pageable));
    }

    @Test
    void getById_whenValid_shouldReturn() {
        when(carteAConteRepository.findByIdAndStatutModeration(
                carteValide.getId(), StatutModeration.valide))
                .thenReturn(Optional.of(carteValide));

        GalerieItemResponse result = galerieService.getCarteGalerie(carteValide.getId());

        assertNotNull(result);
        assertEquals(carteValide.getId(), result.getId());
        assertEquals("Le petit prince", result.getTexteAssocie());
        assertEquals("Amine", result.getCreateurPrenom());
    }

    @Test
    void getById_whenNotValid_shouldThrow404() {
        UUID id = UUID.randomUUID();
        when(carteAConteRepository.findByIdAndStatutModeration(id, StatutModeration.valide))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> galerieService.getCarteGalerie(id));
    }

    @Test
    void getById_whenNotFound_shouldThrow404() {
        UUID id = UUID.randomUUID();
        when(carteAConteRepository.findByIdAndStatutModeration(id, StatutModeration.valide))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> galerieService.getCarteGalerie(id));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    void ajouterMiseEnAvant_whenCarteValide_shouldSave() {
        GalerieMiseEnAvantRequest request = GalerieMiseEnAvantRequest.builder()
                .carteAConteId(carteValide.getId())
                .ordreAffichage(1)
                .build();

        when(carteAConteRepository.findByIdAndStatutModeration(
                carteValide.getId(), StatutModeration.valide))
                .thenReturn(Optional.of(carteValide));

        GalerieMiseEnAvant saved = GalerieMiseEnAvant.builder()
                .id(UUID.randomUUID())
                .carteAConte(carteValide)
                .dateDebut(OffsetDateTime.now())
                .ordreAffichage(1)
                .actif(true)
                .build();

        when(galerieMiseEnAvantRepository.save(any(GalerieMiseEnAvant.class)))
                .thenReturn(saved);

        GalerieMiseEnAvantResponse result = galerieService.ajouterMiseEnAvant(request);

        assertNotNull(result);
        assertEquals(carteValide.getId(), result.getCarteAConteId());
        assertEquals(1, result.getOrdreAffichage());
        assertTrue(result.isActif());
        verify(galerieMiseEnAvantRepository).save(any(GalerieMiseEnAvant.class));
    }

    @Test
    void ajouterMiseEnAvant_whenCarteNonValide_shouldThrow() {
        UUID nonValideId = UUID.randomUUID();
        GalerieMiseEnAvantRequest request = GalerieMiseEnAvantRequest.builder()
                .carteAConteId(nonValideId)
                .ordreAffichage(1)
                .build();

        when(carteAConteRepository.findByIdAndStatutModeration(
                nonValideId, StatutModeration.valide))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> galerieService.ajouterMiseEnAvant(request));
        verify(galerieMiseEnAvantRepository, never()).save(any());
    }
}
