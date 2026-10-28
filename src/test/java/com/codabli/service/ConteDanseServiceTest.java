package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutConte;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.event.ConteDansePublieEvent;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
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
import static org.mockito.Mockito.argThat;
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
        conte.setTitre("Le roi des oiseaux");
        conte.setStatut(StatutConte.brouillon);
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

    // ── Titre facultatif en brouillon (parcours "Creer mon conte danse") ──

    @Test
    void creer_sansTitre_shouldCreateBrouillon() {
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(conteDanseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conteDanseService.creer(conteDanseRequest(null), jwt);

        verify(conteDanseRepository).save(argThat(saved -> saved.getTitre() == null));
    }

    @Test
    void modifier_brouillonSansTitre_shouldBeAllowed() {
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));
        when(conteDanseRepository.save(conte)).thenReturn(conte);

        conteDanseService.modifier(conteId, conteDanseRequest(" "), jwt);

        assertEquals(" ", conte.getTitre());
    }

    @Test
    void modifier_conteSortiDuBrouillonSansTitre_shouldThrowTitreObligatoire() {
        conte.setStatut(StatutConte.publie);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> conteDanseService.modifier(conteId, conteDanseRequest(null), jwt));

        assertEquals(ErrorCode.TITRE_CONTE_OBLIGATOIRE, ex.getErrorCode());
        assertEquals("Le roi des oiseaux", conte.getTitre());
        verify(conteDanseRepository, never()).save(any());
    }

    @Test
    void soumettre_sansTitre_shouldThrowTitreObligatoire() {
        conte.setTitre(null);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        BusinessException ex = assertThrows(BusinessException.class, () -> conteDanseService.soumettre(conteId, jwt));

        assertEquals(ErrorCode.TITRE_CONTE_OBLIGATOIRE, ex.getErrorCode());
        assertEquals(StatutConte.brouillon, conte.getStatut());
        verify(conteDanseRepository, never()).save(any());
    }

    @Test
    void soumettre_avecTitre_shouldSendToTeacherReview() {
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));
        when(conteDanseRepository.save(conte)).thenReturn(conte);

        conteDanseService.soumettre(conteId, jwt);

        assertEquals(StatutConte.en_revision_enseignant, conte.getStatut());
    }

    @Test
    void valider_asAdmin_brouillonSansTitre_shouldNotPublish() {
        conte.setTitre("");
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("admin"));
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        BusinessException ex = assertThrows(BusinessException.class, () -> conteDanseService.valider(conteId, jwt));

        assertEquals(ErrorCode.TITRE_CONTE_OBLIGATOIRE, ex.getErrorCode());
        assertEquals(StatutConte.brouillon, conte.getStatut());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private ConteDanseRequest conteDanseRequest() {
        return conteDanseRequest("titre");
    }

    private ConteDanseRequest conteDanseRequest(String titre) {
        return new ConteDanseRequest(titre, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
