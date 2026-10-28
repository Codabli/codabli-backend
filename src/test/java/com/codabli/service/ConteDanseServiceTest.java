package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutConte;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.event.ConteDansePublieEvent;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.ConteDanseTraductionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour ConteDanseService.
 */
@ExtendWith(MockitoExtension.class)
class ConteDanseServiceTest {

    @Mock
    private ConteDanseRepository conteDanseRepository;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private ConteDanseTraductionRepository traductionRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ConteDanseService conteDanseService;

    private Utilisateur auteur;
    private Utilisateur autreUser;
    private ConteDanse conte;
    private UUID conteId;

    @BeforeEach
    void setUp() {
        auteur = new Utilisateur();
        auteur.setId(UUID.randomUUID());

        autreUser = new Utilisateur();
        autreUser.setId(UUID.randomUUID());

        conteId = UUID.randomUUID();
        conte = new ConteDanse();
        conte.setId(conteId);
        conte.setCreateur(auteur);
    }

    @Test
    void modifier_withNonAuteurNonAdmin_shouldThrowAccessDenied() {
        ConteDanseRequest request = conteDanseRequest();

        when(utilisateurService.getUtilisateurFromJwt(any())).thenReturn(autreUser);
        when(utilisateurService.extractRoles(any())).thenReturn(List.of()); // no roles
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        assertThrows(AccessDeniedException.class, () -> conteDanseService.modifier(conteId, request, jwt));

        verify(conteDanseRepository, never()).save(any());
    }

    @Test
    void filtrerPublies_shouldUseSpecificationForOptionalFilters() {
        when(conteDanseRepository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<ConteDanse>>any(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Page.empty());

        conteDanseService.filtrerPublies(null, null, "amitie", null, null, PageRequest.of(0, 10));
        conteDanseService.filtrerPublies(null, "Liban", null, null, null, PageRequest.of(0, 10));
        conteDanseService.filtrerPublies(null, "Liban", "amitie", null, null, PageRequest.of(0, 10));
        conteDanseService.filtrerPublies("", " ", "", null, null, PageRequest.of(0, 10));

        verify(conteDanseRepository, times(4))
                .findAll(
                        org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<ConteDanse>>any(),
                        any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void listerPublies_shouldLoadPublishedTranslationsInOneBatch() {
        conte.setStatut(com.codabli.entity.enums.StatutConte.publie);
        when(conteDanseRepository.findByStatutOrderByDatePublicationDesc(
                eq(com.codabli.entity.enums.StatutConte.publie), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conte)));
        when(traductionRepository.findByConteIdInAndStatut(
                List.of(conteId), com.codabli.entity.enums.StatutTraduction.publiee))
                .thenReturn(List.of());

        Page<?> result = conteDanseService.listerPublies(PageRequest.of(0, 10));

        assertEquals(1L, result.getTotalElements());
        verify(traductionRepository).findByConteIdInAndStatut(
                List.of(conteId), StatutTraduction.publiee);
    }

    @Test
    void getById_shouldIncludePublishedTranslationsForPublishedConte() {
        conte.setStatut(StatutConte.publie);
        ConteDanseTraduction traduction = new ConteDanseTraduction();
        traduction.setId(UUID.randomUUID());
        traduction.setConte(conte);
        traduction.setLangue("fr");
        traduction.setStatut(StatutTraduction.publiee);

        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));
        when(traductionRepository.findByConteIdAndStatut(conteId, StatutTraduction.publiee))
                .thenReturn(List.of(traduction));

        var response = conteDanseService.getById(conteId);

        assertEquals(1, response.traductions().size());
        assertEquals("fr", response.traductions().getFirst().langue());
    }

    @Test
    void getById_shouldNotExposeUnpublishedConte() {
        conte.setStatut(StatutConte.brouillon);
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        assertThrows(ResourceNotFoundException.class, () -> conteDanseService.getById(conteId));

        verifyNoInteractions(traductionRepository);
    }

    @Test
    void rechercher_shouldIncludePublishedTranslationsInBatch() {
        conte.setStatut(StatutConte.publie);
        when(conteDanseRepository.rechercherPublies(
                eq(StatutConte.publie), eq("danse"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conte)));
        when(traductionRepository.findByConteIdInAndStatut(List.of(conteId), StatutTraduction.publiee))
                .thenReturn(List.of());

        Page<?> response = conteDanseService.rechercher("danse", PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        verify(traductionRepository).findByConteIdInAndStatut(List.of(conteId), StatutTraduction.publiee);
    }

    @Test
    void valider_asAdmin_shouldPublishEventWhenConteIsPublished() {
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("admin"));
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));
        when(conteDanseRepository.save(conte)).thenReturn(conte);

        conteDanseService.valider(conteId, jwt);

        assertEquals(StatutConte.publie, conte.getStatut());
        verify(eventPublisher).publishEvent(new ConteDansePublieEvent(conteId));
    }

    private ConteDanseRequest conteDanseRequest() {
        return new ConteDanseRequest("titre", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
