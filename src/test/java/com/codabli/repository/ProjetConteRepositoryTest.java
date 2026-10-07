package com.codabli.repository;

import com.codabli.entity.ProjetConte;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.entity.enums.RoleUtilisateur;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration (H2) du mapping JPA de ProjetConte.
 */
@SpringBootTest
@Transactional
class ProjetConteRepositoryTest {

    @Autowired
    private ProjetConteRepository projetConteRepository;

    @Autowired
    private EntityManager entityManager;

    private Utilisateur enseignant;

    @BeforeEach
    void setUp() {
        enseignant = enseignant("claire.martin@codabli.dev");
    }

    @Test
    void save_shouldPersistContextWithOrderedIngredients() {
        ProjetConte projet = projetConteRepository.save(projet(enseignant, "Le Moyen Age",
                List.of("chateaux forts", "dragons", "voyage dans le temps")));
        entityManager.flush();
        entityManager.clear();

        ProjetConte relu = projetConteRepository.findById(projet.getId()).orElseThrow();

        assertEquals(enseignant.getId(), relu.getEnseignant().getId());
        assertEquals("9-11", relu.getTrancheAge());
        assertEquals("Centre-Val de Loire", relu.getRegion());
        assertEquals(EspaceRepresentation.salle_spectacle, relu.getEspaceRepresentation());
        assertEquals(List.of("chateaux forts", "dragons", "voyage dans le temps"), relu.getIngredientsSecrets());
        assertNotNull(relu.getDateCreation());
        assertNotNull(relu.getDateModification());
    }

    @Test
    void update_shouldReplaceIngredientsAndRefreshModificationDate() throws InterruptedException {
        ProjetConte projet = projetConteRepository.save(projet(enseignant, "Nature", List.of("la mer", "le vent")));
        entityManager.flush();
        var dateModificationInitiale = projet.getDateModification();
        Thread.sleep(10);

        projet.getIngredientsSecrets().clear();
        projet.getIngredientsSecrets().add("la foret");
        projet.setTheme("La foret");
        projetConteRepository.save(projet);
        entityManager.flush();
        entityManager.clear();

        ProjetConte relu = projetConteRepository.findById(projet.getId()).orElseThrow();

        assertEquals(List.of("la foret"), relu.getIngredientsSecrets());
        assertEquals("La foret", relu.getTheme());
        assertTrue(relu.getDateModification().isAfter(dateModificationInitiale));
    }

    @Test
    void findByEnseignant_shouldReturnOnlyTeacherProjectsMostRecentFirst() throws InterruptedException {
        Utilisateur autreEnseignant = enseignant("paul.durand@codabli.dev");
        ProjetConte ancien = projetConteRepository.save(projet(enseignant, "Ancien", List.of()));
        entityManager.flush();
        Thread.sleep(10);
        ProjetConte recent = projetConteRepository.save(projet(enseignant, "Recent", List.of()));
        projetConteRepository.save(projet(autreEnseignant, "Autre", List.of()));
        entityManager.flush();

        List<ProjetConte> projets = projetConteRepository
                .findByEnseignantIdOrderByDateModificationDesc(enseignant.getId());

        assertEquals(List.of(recent.getId(), ancien.getId()), projets.stream().map(ProjetConte::getId).toList());
    }

    private Utilisateur enseignant(String email) {
        Utilisateur utilisateur = Utilisateur.builder()
                .keycloakId(UUID.randomUUID().toString())
                .nom("Martin")
                .prenom("Claire")
                .email(email)
                .role(RoleUtilisateur.enseignant)
                .build();
        entityManager.persist(utilisateur);
        return utilisateur;
    }

    private ProjetConte projet(Utilisateur proprietaire, String theme, List<String> ingredients) {
        return ProjetConte.builder()
                .enseignant(proprietaire)
                .trancheAge("9-11")
                .pays("France")
                .region("Centre-Val de Loire")
                .ville("Tours")
                .theme(theme)
                .ingredientsSecrets(new ArrayList<>(ingredients))
                .espaceRepresentation(EspaceRepresentation.salle_spectacle)
                .build();
    }
}
