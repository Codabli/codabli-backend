package com.codabli.service;

import com.codabli.dto.ConteDanseTraductionRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ConteDanseTraductionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConteDanseTraductionServiceTest {

    @Mock
    private ConteDanseTraductionRepository traductionRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ConteDanseService conteDanseService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ConteDanseTraductionService traductionService;

    private UUID conteId;
    private UUID traductionId;
    private ConteDanseTraduction traduction;

    @BeforeEach
    void setUp() {
        conteId = UUID.randomUUID();
        traductionId = UUID.randomUUID();
        ConteDanse conte = new ConteDanse();
        conte.setId(conteId);

        traduction = new ConteDanseTraduction();
        traduction.setId(traductionId);
        traduction.setConte(conte);
        traduction.setLangue("fr");
        traduction.setStatut(StatutTraduction.publiee);
    }

    @Test
    void modifier_shouldRequireTheTranslationToBelongToTheConte() {
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));

        assertThrows(ResourceNotFoundException.class,
                () -> traductionService.modifier(UUID.randomUUID(), traductionId, request()));

        verify(traductionRepository, never()).save(any());
    }

    @Test
    void modifier_shouldResetPublishedTranslationToAwaitingTranslation() {
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(traductionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = traductionService.modifier(conteId, traductionId, request());

        assertEquals(StatutTraduction.a_traduire, response.statut());
        verify(traductionRepository).save(traduction);
    }

    @Test
    void changerStatut_shouldAllowTranslatorToAdvanceTranslationToReview() {
        traduction.setStatut(StatutTraduction.en_cours);
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("traducteur"));
        when(traductionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = traductionService.changerStatut(
                conteId, traductionId, StatutTraduction.relecture, jwt);

        assertEquals(StatutTraduction.relecture, response.statut());
        verify(traductionRepository).save(traduction);
    }

    @Test
    void changerStatut_shouldRejectPublishingBeforeValidation() {
        traduction.setStatut(StatutTraduction.relecture);
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("comite_lecture"));

        BusinessException exception = assertThrows(BusinessException.class, () -> traductionService.changerStatut(
                conteId, traductionId, StatutTraduction.publiee, jwt));

        assertEquals(ErrorCode.INVALID_TRANSLATION_TRANSITION, exception.getErrorCode());
        verify(traductionRepository, never()).save(any());
    }

    @Test
    void changerStatut_shouldAllowCommitteeToPublishValidatedTranslation() {
        traduction.setStatut(StatutTraduction.validee);
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("comite_lecture"));
        when(traductionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = traductionService.changerStatut(
                conteId, traductionId, StatutTraduction.publiee, jwt);

        assertEquals(StatutTraduction.publiee, response.statut());
        verify(traductionRepository).save(traduction);
    }

    @Test
    void changerStatut_shouldNotAllowTranslatorToValidate() {
        traduction.setStatut(StatutTraduction.relecture);
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("traducteur"));

        assertThrows(BusinessException.class, () -> traductionService.changerStatut(
                conteId, traductionId, StatutTraduction.validee, jwt));

        verify(traductionRepository, never()).save(any());
    }

    @Test
    void changerStatut_shouldRejectUserWithoutTranslationRole() {
        when(traductionRepository.findById(traductionId)).thenReturn(Optional.of(traduction));
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("eleve"));

        assertThrows(AccessDeniedException.class, () -> traductionService.changerStatut(
                conteId, traductionId, StatutTraduction.en_cours, jwt));

        verify(traductionRepository, never()).save(any());
    }

    private ConteDanseTraductionRequest request() {
        return new ConteDanseTraductionRequest("fr", null, "Texte traduit", null, null, null, null, null);
    }
}
