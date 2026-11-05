package com.codabli.service;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.parcours.CarteUnivers;
import com.codabli.dto.parcours.DanseScene;
import com.codabli.dto.parcours.EcritureParcours;
import com.codabli.dto.parcours.EtapeTrame;
import com.codabli.dto.parcours.MiseEnSceneParcours;
import com.codabli.dto.parcours.MomentScene;
import com.codabli.dto.parcours.PersonnageScene;
import com.codabli.dto.parcours.PersonnageUnivers;
import com.codabli.dto.parcours.PlacementScene;
import com.codabli.dto.parcours.SceneMiseEnScene;
import com.codabli.dto.parcours.SonScene;
import com.codabli.dto.parcours.TrameParcours;
import com.codabli.dto.parcours.UniversParcours;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.service.ConducteurSpectacle.Danse;
import com.codabli.service.ConducteurSpectacle.ElementPlateau;
import com.codabli.service.ConducteurSpectacle.Scene;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConducteurSpectacleServiceTest {

    private static final UUID CONTE_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);

    @Mock
    private ParcoursConteService parcoursConteService;

    @Mock
    private ConteDanseService conteDanseService;

    @Spy
    private ConducteurPdfGenerator pdfGenerator = new ConducteurPdfGenerator();

    @InjectMocks
    private ConducteurSpectacleService service;

    @Test
    void construire_shouldFillCoverAndDistribution() {
        ConducteurSpectacle conducteur = service.construire(conte(), parcours(), DATE);

        assertEquals("Arthur et le château de Chinon", conducteur.titre());
        assertEquals("Le Moyen Âge", conducteur.theme());
        assertEquals("6 à 8 ans", conducteur.ages());
        assertEquals("Chinon, Centre-Val de Loire, France", conducteur.lieu());
        assertEquals("Salle de spectacle", conducteur.espace());
        assertEquals("2 novembre 2026", conducteur.dateEdition());
        assertEquals(List.of(new ConducteurSpectacle.Personnage("Arthur", "Héros"),
                new ConducteurSpectacle.Personnage("Merlin", "Mentor")), conducteur.distribution());
    }

    @Test
    void construire_shouldFollowTheStoryStructure() {
        ConducteurSpectacle conducteur = service.construire(conte(), parcours(), DATE);

        assertEquals(List.of("Le rêve d'Arthur", "Le départ"),
                conducteur.scenes().stream().map(Scene::titre).toList());
        Scene premiere = conducteur.scenes().getFirst();
        assertEquals(1, premiere.numero());
        assertEquals("<p>Il était une fois <strong>Arthur</strong>.</p>", premiere.texteHtml());
        assertEquals("Sur scène dès le début", premiere.presences().getFirst().entree());
        assertEquals("Sort pendant la scène : quand Merlin l'appelle", premiere.presences().getFirst().sortie());
        assertEquals("Entre pendant la scène", premiere.presences().get(1).entree());
    }

    @Test
    void construire_sceneWithoutStaging_shouldStayEmpty() {
        Scene seconde = service.construire(conte(), parcours(), DATE).scenes().get(1);

        assertNull(seconde.texteHtml());
        assertNull(seconde.intention());
        assertTrue(seconde.presences().isEmpty());
        assertTrue(seconde.danses().isEmpty());
    }

    @Test
    void construire_shouldDescribeDancesAndOnlyKeepPresentElements() {
        Danse danse = service.construire(conte(), parcours(), DATE).scenes().getFirst().danses().getFirst();

        assertEquals("Toute la scène", danse.moment());
        assertEquals("Traditionnel", danse.style());
        assertEquals("Musique", danse.musiques().getFirst().type());
        assertEquals("Début", danse.musiques().getFirst().moment());
        // "fantome" n'est plus dans l'univers et "une lance" n'est plus dans les decors de la scene
        assertEquals(List.of(
                        new ElementPlateau("Arthur", true, 20.0, 50.0, "Milieu, côté jardin", "A", "#1f7a55"),
                        new ElementPlateau("Merlin", true, 50.0, 90.0, "Avant-scène, au centre", "M", "#4b4bc4"),
                        new ElementPlateau("un arbre", false, 80.0, 10.0, "Fond, côté cour", "1", null)),
                danse.plateau());
    }

    @Test
    void construire_shouldCleanTheStoryText() {
        ParcoursConteResponse parcours = parcours(new EcritureParcours(Map.of(
                "e1", "<p onclick=\"x()\">Bonjour<script>alert(1)</script> <img src=x></p>")));

        Scene premiere = service.construire(conte(), parcours, DATE).scenes().getFirst();

        assertEquals("<p>Bonjour</p>", premiere.texteHtml());
    }

    @Test
    void zone_shouldMatchTheFrontStageZones() {
        assertEquals("Fond, côté jardin", ConducteurSpectacleService.zone(10, 10));
        assertEquals("Centre de la scène", ConducteurSpectacleService.zone(50, 50));
        assertEquals("Milieu, côté cour", ConducteurSpectacleService.zone(90, 79.9));
        assertEquals("Avant-scène, côté cour", ConducteurSpectacleService.zone(90, 80));
    }

    @Test
    void nomFichier_shouldBeASlugOfTheTitle() {
        assertEquals("conducteur-arthur-et-le-chateau-de-chinon.pdf",
                ConducteurSpectacleService.nomFichier("Arthur et le château de Chinon !"));
        assertEquals("conducteur-conte.pdf", ConducteurSpectacleService.nomFichier(null));
    }

    @Test
    void genererPdf_shouldRenderACoverAndOnePagePerScene() throws IOException {
        Jwt jwt = mock(Jwt.class);
        when(parcoursConteService.lire(CONTE_ID, jwt)).thenReturn(parcours());
        when(conteDanseService.findById(CONTE_ID)).thenReturn(conte());

        ConducteurSpectacleService.ConducteurPdf pdf = service.genererPdf(CONTE_ID, jwt);

        // Exemple a ouvrir pour verifier la mise en page
        Files.write(Path.of("target", "conducteur-exemple.pdf"), pdf.contenu());
        assertEquals("conducteur-arthur-et-le-chateau-de-chinon.pdf", pdf.nomFichier());
        try (PDDocument document = Loader.loadPDF(pdf.contenu())) {
            // page de garde + au moins une page par scene (une danse trop longue passe a la page suivante)
            assertTrue(document.getNumberOfPages() >= 3);
            String texte = new PDFTextStripper().getText(document);
            assertTrue(texte.contains("Arthur et le château de Chinon"));
            assertTrue(texte.contains("Le rêve d'Arthur"));
            assertTrue(texte.contains("Arthur : Milieu, côté jardin"));
            assertTrue(texte.contains("Non renseigné"));
        }
    }

    @Test
    void genererPdf_emptyParcours_shouldStillRenderTheCover() throws IOException {
        Jwt jwt = mock(Jwt.class);
        ConteDanse conte = ConteDanse.builder().build();
        when(parcoursConteService.lire(CONTE_ID, jwt)).thenReturn(new ParcoursConteResponse(CONTE_ID,
                null, null, null, List.of(), null, null, null, null, null, null, null, null));
        when(conteDanseService.findById(CONTE_ID)).thenReturn(conte);

        ConducteurSpectacleService.ConducteurPdf pdf = service.genererPdf(CONTE_ID, jwt);

        assertEquals("conducteur-conte.pdf", pdf.nomFichier());
        try (PDDocument document = Loader.loadPDF(pdf.contenu())) {
            assertEquals(1, document.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(document).contains("La trame du récit n'est pas encore renseignée."));
        }
    }

    private ConteDanse conte() {
        return ConteDanse.builder()
                .titre("Arthur et le château de Chinon")
                .thematique("Le Moyen Âge")
                .pays("France")
                .ageMin(6)
                .ageMax(8)
                .build();
    }

    private ParcoursConteResponse parcours() {
        return parcours(new EcritureParcours(Map.of(
                "e1", "<p>Il était une fois <strong>Arthur</strong>.</p>",
                "e2", "<p> </p>")));
    }

    private ParcoursConteResponse parcours(EcritureParcours ecriture) {
        UniversParcours univers = new UniversParcours(
                List.of(new CarteUnivers("l1", "Le château", "", null)),
                List.of(new PersonnageUnivers("p1", "Arthur", "Un jeune écuyer", null, "Héros", "Devenir chevalier"),
                        new PersonnageUnivers("p2", "Merlin", "", null, "Mentor", "")),
                List.of(), List.of());
        TrameParcours trame = new TrameParcours(List.of(
                new EtapeTrame("e1", EtapeTrame.TypeEtape.initialSituation, "Le rêve d'Arthur",
                        "Arthur rêve de devenir chevalier.", List.of("p1")),
                new EtapeTrame("e2", EtapeTrame.TypeEtape.trigger, "Le départ", "", List.of())));

        SonScene musique = new SonScene("s1", "Danse médiévale", SonScene.TypeSon.music, MomentScene.beginning,
                "d1", "f1", "danse.mp3");
        DanseScene danse = new DanseScene("d1", "ronde", "La ronde du château", null, DanseScene.Style.traditional,
                List.of(new PlacementScene(PlacementScene.TypeElement.character, "p1", 20.0, 50.0),
                        new PlacementScene(PlacementScene.TypeElement.character, "p2", 50.0, 90.0),
                        new PlacementScene(PlacementScene.TypeElement.character, "fantome", 70.0, 50.0),
                        new PlacementScene(PlacementScene.TypeElement.prop, "un arbre", 80.0, 10.0),
                        new PlacementScene(PlacementScene.TypeElement.prop, "une lance", 30.0, 30.0)),
                List.of(musique));
        Map<String, SceneMiseEnScene> scenes = new LinkedHashMap<>();
        scenes.put("e1", new SceneMiseEnScene(
                List.of(new PersonnageScene("p1", PersonnageScene.Entree.onStage, null,
                                PersonnageScene.Sortie.exits, "quand Merlin l'appelle"),
                        new PersonnageScene("p2", PersonnageScene.Entree.enters, " ",
                                PersonnageScene.Sortie.stays, null),
                        new PersonnageScene("fantome", PersonnageScene.Entree.onStage, null,
                                PersonnageScene.Sortie.stays, null)),
                "Montrer l'impatience d'Arthur", List.of("un arbre", "un trône"),
                List.of(danse),
                List.of(new SonScene("s2", "Cloches", SonScene.TypeSon.effect, null, null, "f2", "cloches.mp3"))));

        return new ParcoursConteResponse(CONTE_ID, "Centre-Val de Loire", "Chinon",
                EspaceRepresentation.salle_spectacle, List.of(), null, null, null,
                univers, trame, ecriture, new MiseEnSceneParcours(scenes), null);
    }
}
