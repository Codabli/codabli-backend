package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ConteDanseService.
 */
@ExtendWith(MockitoExtension.class)
class ConteDanseServiceTest {

    @Mock
    private ConteDanseRepository conteDanseRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

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
        ConteDanseRequest request = new ConteDanseRequest();

        when(jwt.getSubject()).thenReturn("other-id");
        when(utilisateurRepository.findByKeycloakId("other-id"))
                .thenReturn(Optional.of(autreUser));
        when(jwt.getClaimAsMap("realm_access")).thenReturn(Collections.emptyMap()); // no roles
        when(conteDanseRepository.findById(conteId)).thenReturn(Optional.of(conte));

        assertThrows(AccessDeniedException.class, () -> {
            conteDanseService.modifier(conteId, request, jwt);
        });

        verify(conteDanseRepository, never()).save(any());
    }
}
