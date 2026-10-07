package com.codabli.service;

import com.codabli.dto.ProjetConteRequest;
import com.codabli.dto.ProjetConteResponse;
import com.codabli.entity.ProjetConte;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.repository.ProjetConteRepository;
import com.codabli.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ProjetConteService.
 */
@ExtendWith(MockitoExtension.class)
class ProjetConteServiceTest {

    private static final String KEYCLOAK_ID = "keycloak-id-enseignant";

    @Mock
    private ProjetConteRepository projetConteRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ProjetConteService projetConteService;

    @Mock
    private Jwt jwt;

    private Utilisateur enseignant;

    @BeforeEach
    void setUp() {
        enseignant = new Utilisateur();
        enseignant.setId(UUID.randomUUID());
        enseignant.setNom("Martin");
        enseignant.setPrenom("Claire");

        when(jwt.getSubject()).thenReturn(KEYCLOAK_ID);
        when(utilisateurRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(enseignant));
    }

    @Test
    void creer_shouldAttachTeacherAndNormalizeContext() {
        ProjetConteRequest request = new ProjetConteRequest(
                "9-11", " France ", " Centre-Val de Loire ", "  ", " Le Moyen Age ",
                List.of(" dragons ", "Chateaux forts", "Dragons"),
                EspaceRepresentation.gymnase);
        when(projetConteRepository.save(any(ProjetConte.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjetConteResponse response = projetConteService.creer(request, jwt);

        assertEquals(enseignant.getId(), response.enseignantId());
        assertEquals("9-11", response.trancheAge());
        assertEquals("France", response.pays());
        assertEquals("Centre-Val de Loire", response.region());
        assertNull(response.ville());
        assertEquals("Le Moyen Age", response.theme());
        assertEquals(List.of("dragons", "Chateaux forts"), response.ingredientsSecrets());
        assertEquals(EspaceRepresentation.gymnase, response.espaceRepresentation());
        verify(projetConteRepository, times(1)).save(any(ProjetConte.class));
    }

    @Test
    void creer_withoutIngredients_shouldReturnEmptyList() {
        ProjetConteRequest request = new ProjetConteRequest(
                "3-5", "France", "Bretagne", null, "Nature", null, null);
        when(projetConteRepository.save(any(ProjetConte.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjetConteResponse response = projetConteService.creer(request, jwt);

        assertEquals(List.of(), response.ingredientsSecrets());
        assertNull(response.espaceRepresentation());
    }

    @Test
    void modifierContexte_shouldReplaceContextOfOwnProject() {
        ProjetConte projet = projetDe(enseignant);
        when(projetConteRepository.findById(projet.getId())).thenReturn(Optional.of(projet));
        when(projetConteRepository.save(projet)).thenReturn(projet);

        ProjetConteResponse response = projetConteService.modifierContexte(projet.getId(),
                new ProjetConteRequest("12-14", "Belgique", "Wallonie", "Namur", "Les emotions",
                        List.of("joie"), EspaceRepresentation.classe),
                jwt);

        assertEquals("12-14", response.trancheAge());
        assertEquals("Belgique", response.pays());
        assertEquals(List.of("joie"), response.ingredientsSecrets());
        assertEquals(EspaceRepresentation.classe, response.espaceRepresentation());
    }

    @Test
    void modifierContexte_ofAnotherTeacherProject_shouldThrowAccessDenied() {
        Utilisateur autreEnseignant = new Utilisateur();
        autreEnseignant.setId(UUID.randomUUID());
        ProjetConte projet = projetDe(autreEnseignant);
        when(projetConteRepository.findById(projet.getId())).thenReturn(Optional.of(projet));

        ProjetConteRequest request = new ProjetConteRequest(
                "6-8", "France", "Bretagne", null, "Nature", List.of(), null);

        assertThrows(AccessDeniedException.class,
                () -> projetConteService.modifierContexte(projet.getId(), request, jwt));
        verify(projetConteRepository, never()).save(any(ProjetConte.class));
    }

    @Test
    void consulter_unknownProject_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(projetConteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projetConteService.consulter(id, jwt));
    }

    @Test
    void listerMesProjets_shouldOnlyQueryConnectedTeacherProjects() {
        when(projetConteRepository.findByEnseignantIdOrderByDateModificationDesc(enseignant.getId()))
                .thenReturn(List.of(projetDe(enseignant)));

        List<ProjetConteResponse> projets = projetConteService.listerMesProjets(jwt);

        assertEquals(1, projets.size());
        assertEquals(enseignant.getId(), projets.get(0).enseignantId());
    }

    private ProjetConte projetDe(Utilisateur proprietaire) {
        return ProjetConte.builder()
                .id(UUID.randomUUID())
                .enseignant(proprietaire)
                .trancheAge("6-8")
                .pays("France")
                .region("Bretagne")
                .theme("Fantastique")
                .ingredientsSecrets(new ArrayList<>(List.of("dragons")))
                .build();
    }
}
