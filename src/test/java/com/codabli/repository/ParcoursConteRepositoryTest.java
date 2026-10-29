package com.codabli.repository;

import com.codabli.entity.ConteDanse;
import com.codabli.entity.ParcoursConte;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.entity.enums.EtapeParcours;
import com.codabli.entity.enums.RoleUtilisateur;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParcoursConteRepositoryTest {

    @Autowired
    private ParcoursConteRepository repository;

    @Autowired
    private ConteDanseRepository conteRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_shouldRoundTripContextAndJsonSteps() {
        UUID conteDanseId = creerConte();
        ParcoursConte parcours = new ParcoursConte();
        parcours.setConteDanseId(conteDanseId);
        parcours.setRegion("Bretagne");
        parcours.setEspaceRepresentation(EspaceRepresentation.gymnase);
        parcours.setIngredientsSecrets(List.of("dragons", "châteaux forts"));
        parcours.setEtape(EtapeParcours.univers, Map.of(
                "places", List.of(Map.of("id", "p1", "name", "La forêt")),
                "characters", List.of()));
        repository.saveAndFlush(parcours);
        entityManager.clear();

        ParcoursConte relu = repository.findByConteDanseId(conteDanseId).orElseThrow();

        assertEquals("Bretagne", relu.getRegion());
        assertEquals(EspaceRepresentation.gymnase, relu.getEspaceRepresentation());
        assertEquals(List.of("dragons", "châteaux forts"), relu.getIngredientsSecrets());
        assertEquals(List.of(Map.of("id", "p1", "name", "La forêt")), relu.getUnivers().get("places"));
        assertTrue(((List<?>) relu.getUnivers().get("characters")).isEmpty());
        assertNull(relu.getTrame());
        assertNotNull(relu.getDateCreation());
        assertNotNull(relu.getDateModification());
    }

    @Test
    void save_secondParcoursForSameConte_shouldBeRejected() {
        UUID conteDanseId = creerConte();
        repository.saveAndFlush(nouveauParcours(conteDanseId));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(nouveauParcours(conteDanseId)));
    }

    @Test
    void deleteConte_shouldDeleteItsParcours() {
        UUID conteDanseId = creerConte();
        repository.saveAndFlush(nouveauParcours(conteDanseId));
        entityManager.clear();

        conteRepository.deleteById(conteDanseId);
        conteRepository.flush();
        entityManager.clear();

        assertTrue(repository.findByConteDanseId(conteDanseId).isEmpty());
    }

    private ParcoursConte nouveauParcours(UUID conteDanseId) {
        ParcoursConte parcours = new ParcoursConte();
        parcours.setConteDanseId(conteDanseId);
        return parcours;
    }

    private UUID creerConte() {
        Utilisateur auteur = utilisateurRepository.saveAndFlush(Utilisateur.builder()
                .nom("Auteur").prenom("Test").role(RoleUtilisateur.enseignant).build());
        // Brouillon sans titre : cas du parcours, cree des l'ecran 1
        return conteRepository.saveAndFlush(ConteDanse.builder().createur(auteur).build()).getId();
    }
}
