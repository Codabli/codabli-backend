package com.codabli.service;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.ParcoursContexteRequest;
import com.codabli.dto.parcours.EcritureParcours;
import com.codabli.dto.parcours.EtapeTrame;
import com.codabli.dto.parcours.RessourcesClasseParcours;
import com.codabli.dto.parcours.TrameParcours;
import com.codabli.dto.parcours.UniversParcours;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ParcoursConte;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.entity.enums.StatutConte;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.ParcoursConteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ParcoursConteService.
 */
@ExtendWith(MockitoExtension.class)
class ParcoursConteServiceTest {

    @Mock
    private ParcoursConteRepository parcoursRepository;

    @Mock
    private ConteDanseService conteDanseService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private Jwt jwt;

    private ParcoursConteService parcoursConteService;

    private Utilisateur auteur;
    private ConteDanse conte;
    private UUID conteId;

    @BeforeEach
    void setUp() {
        auteur = new Utilisateur();
        auteur.setId(UUID.randomUUID());

        conteId = UUID.randomUUID();
        conte = new ConteDanse();
        conte.setId(conteId);
        conte.setCreateur(auteur);
        conte.setStatut(StatutConte.brouillon);

        lenient().when(conteDanseService.findById(conteId)).thenReturn(conte);
        lenient().when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        lenient().when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("enseignant"));
        lenient().when(parcoursRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        parcoursConteService = new ParcoursConteService(parcoursRepository, conteDanseService,
                new ConteDanseAccesService(conteDanseService, utilisateurService));
    }

    @Test
    void lire_sansParcoursEnregistre_shouldReturnEmptyParcours() {
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.empty());

        ParcoursConteResponse response = parcoursConteService.lire(conteId, jwt);

        assertEquals(conteId, response.conteDanseId());
        assertNull(response.region());
        assertEquals(List.of(), response.ingredientsSecrets());
        assertNull(response.univers());
    }

    @Test
    void lire_parUnAutreUtilisateur_shouldThrowAccessDenied() {
        Utilisateur autre = new Utilisateur();
        autre.setId(UUID.randomUUID());
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(autre);

        assertThrows(AccessDeniedException.class, () -> parcoursConteService.lire(conteId, jwt));
        verify(parcoursRepository, never()).findByConteDanseId(any());
    }

    @Test
    void lire_parUnModerateur_shouldBeAllowed() {
        Utilisateur moderateur = new Utilisateur();
        moderateur.setId(UUID.randomUUID());
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(moderateur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of("moderateur"));
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.empty());

        assertEquals(conteId, parcoursConteService.lire(conteId, jwt).conteDanseId());
    }

    @Test
    void enregistrerContexte_premierEnregistrement_shouldCreateParcours() {
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.empty());
        ParcoursContexteRequest request = new ParcoursContexteRequest(
                " Bretagne ", "  ", EspaceRepresentation.exterieur, List.of("la mer"));

        ParcoursConteResponse response = parcoursConteService.enregistrerContexte(conteId, request, jwt);

        assertEquals(conteId, response.conteDanseId());
        assertEquals("Bretagne", response.region());
        assertNull(response.ville());
        assertEquals(EspaceRepresentation.exterieur, response.espaceRepresentation());
        assertEquals(List.of("la mer"), response.ingredientsSecrets());
    }

    @Test
    void enregistrerEtape_shouldOnlyReplaceThatStep() {
        ParcoursConte existant = new ParcoursConte();
        existant.setConteDanseId(conteId);
        existant.setRegion("Bretagne");
        UniversParcours univers = new UniversParcours(List.of(), List.of(), List.of(), List.of());
        existant.setUnivers(univers);
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.of(existant));
        TrameParcours trame = new TrameParcours(List.of(
                new EtapeTrame("s1", EtapeTrame.TypeEtape.trigger, "Le départ", "", List.of())));

        ParcoursConteResponse response = parcoursConteService.enregistrerEtape(conteId, trame, jwt);

        assertEquals(trame, response.trame());
        assertEquals(univers, response.univers());
        assertEquals("Bretagne", response.region());
    }

    @Test
    void enregistrerEtape_shouldStoreContentInTheMatchingStep() {
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.empty());
        RessourcesClasseParcours contenu = new RessourcesClasseParcours(List.of());

        ParcoursConteResponse response = parcoursConteService.enregistrerEtape(conteId, contenu, jwt);

        assertEquals(contenu, response.ressourcesClasse());
        assertNull(response.univers());
    }

    @Test
    void enregistrerEtape_conteEnModeration_shouldThrowNonModifiable() {
        conte.setStatut(StatutConte.en_revision_enseignant);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> parcoursConteService.enregistrerEtape(conteId,
                        new UniversParcours(List.of(), List.of(), List.of(), List.of()), jwt));

        assertEquals(ErrorCode.PARCOURS_NON_MODIFIABLE, ex.getErrorCode());
        verify(parcoursRepository, never()).save(any());
    }

    @Test
    void enregistrerEtape_conteRefuse_shouldBeAllowed() {
        conte.setStatut(StatutConte.refuse);
        when(parcoursRepository.findByConteDanseId(conteId)).thenReturn(Optional.empty());

        parcoursConteService.enregistrerEtape(conteId, new EcritureParcours(Map.of()), jwt);

        verify(parcoursRepository).save(any());
    }
}
