package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ConteDanseRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private EntityManager entityManager;

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

    private ConteDanseRequest conteDanseRequest() {
        return new ConteDanseRequest("titre", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
