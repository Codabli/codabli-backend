package com.codabli.service;

import com.codabli.dto.ActualiteRequest;
import com.codabli.dto.ActualiteResponse;
import com.codabli.entity.Actualite;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ActualiteRepository;
import com.codabli.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ActualiteService.
 */
@ExtendWith(MockitoExtension.class)
class ActualiteServiceTest {

    @Mock
    private ActualiteRepository actualiteRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ActualiteService actualiteService;

    @Mock
    private Jwt jwt;

    private Utilisateur mockAdmin;

    @BeforeEach
    void setUp() {
        mockAdmin = new Utilisateur();
        mockAdmin.setId(UUID.randomUUID());
        mockAdmin.setNom("Admin");
        mockAdmin.setPrenom("Test");
    }

    @Test
    void creer_shouldSaveAndReturnResponse() {
        ActualiteRequest request = ActualiteRequest.builder()
                .titre("Titre")
                .contenu("Contenu")
                .publie(true)
                .build();

        when(jwt.getSubject()).thenReturn("keycloak-id-test");
        when(utilisateurRepository.findByKeycloakId("keycloak-id-test"))
                .thenReturn(Optional.of(mockAdmin));

        Actualite savedActualite = new Actualite();
        savedActualite.setId(UUID.randomUUID());
        savedActualite.setTitre("Titre");
        savedActualite.setContenu("Contenu");
        savedActualite.setPublie(true);
        savedActualite.setAuteur(mockAdmin);

        when(actualiteRepository.save(any(Actualite.class))).thenReturn(savedActualite);

        ActualiteResponse response = actualiteService.creer(request, jwt);

        assertNotNull(response);
        assertEquals("Titre", response.getTitre());
        verify(actualiteRepository, times(1)).save(any(Actualite.class));
    }
}
