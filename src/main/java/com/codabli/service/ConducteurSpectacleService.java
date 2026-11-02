package com.codabli.service;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.parcours.DanseScene;
import com.codabli.dto.parcours.EtapeTrame;
import com.codabli.dto.parcours.MomentScene;
import com.codabli.dto.parcours.PersonnageScene;
import com.codabli.dto.parcours.PersonnageUnivers;
import com.codabli.dto.parcours.PlacementScene;
import com.codabli.dto.parcours.SceneMiseEnScene;
import com.codabli.dto.parcours.SonScene;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.service.ConducteurSpectacle.Danse;
import com.codabli.service.ConducteurSpectacle.ElementPlateau;
import com.codabli.service.ConducteurSpectacle.Personnage;
import com.codabli.service.ConducteurSpectacle.Presence;
import com.codabli.service.ConducteurSpectacle.Scene;
import com.codabli.service.ConducteurSpectacle.Son;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Conducteur du spectacle (ecran 8, SCRUM-135) : recapitulatif du parcours
 * d'un conte, scene par scene, pour monter le spectacle avec la classe.
 * <p>
 * Lecture : memes droits que le parcours (auteur du conte, admin ou moderateur).
 */
@Service
@Transactional(readOnly = true)
public class ConducteurSpectacleService {

    private static final ZoneId FUSEAU = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    /** Limite du rideau, en pourcentage de la profondeur : meme valeur que CURTAIN_LINE dans le front. */
    static final double LIGNE_RIDEAU = 80;

    /** Couleurs des personnages sur le plateau, dans l'ordre de la scene : memes que CHARACTER_COLORS dans le front. */
    private static final List<String> COULEURS_PERSONNAGES =
            List.of("#1f7a55", "#4b4bc4", "#a3480a", "#a8326a", "#16679a", "#6342d4");

    // Texte de l'ecran 7 (editeur enrichi) : mise en forme simple et titres, sans image ni style.
    private static final Safelist TEXTE_AUTORISE = Safelist.basic()
            .removeTags("a")
            .addTags("h1", "h2", "h3", "h4", "s", "u");

    private final ParcoursConteService parcoursConteService;
    private final ConteDanseService conteDanseService;
    private final ConducteurPdfGenerator pdfGenerator;

    public ConducteurSpectacleService(ParcoursConteService parcoursConteService,
                                      ConteDanseService conteDanseService,
                                      ConducteurPdfGenerator pdfGenerator) {
        this.parcoursConteService = parcoursConteService;
        this.conteDanseService = conteDanseService;
        this.pdfGenerator = pdfGenerator;
    }

    /** PDF genere et nom du fichier a telecharger. */
    public record ConducteurPdf(String nomFichier, byte[] contenu) {
    }

    /** Conducteur du conte, en PDF. */
    public ConducteurPdf genererPdf(UUID conteDanseId, Jwt jwt) {
        // lire() verifie l'acces au parcours avant de charger quoi que ce soit
        ParcoursConteResponse parcours = parcoursConteService.lire(conteDanseId, jwt);
        ConteDanse conte = conteDanseService.findById(conteDanseId);
        byte[] contenu = pdfGenerator.generer(construire(conte, parcours, LocalDate.now(FUSEAU)));
        return new ConducteurPdf(nomFichier(conte.getTitre()), contenu);
    }

    /** Ex. "conducteur-arthur-et-le-chateau-de-chinon.pdf", "conducteur-conte.pdf" sans titre. */
    static String nomFichier(String titre) {
        String slug = titre == null ? "" : Normalizer.normalize(titre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return "conducteur-" + (slug.isEmpty() ? "conte" : slug) + ".pdf";
    }

    ConducteurSpectacle construire(ConteDanse conte, ParcoursConteResponse parcours, LocalDate dateEdition) {
        List<PersonnageUnivers> personnages = parcours.univers() == null ? List.of() : parcours.univers().characters();
        Map<String, PersonnageUnivers> personnagesParId = personnages.stream()
                .collect(Collectors.toMap(PersonnageUnivers::id, Function.identity(), (premier, autre) -> premier));
        List<EtapeTrame> etapes = parcours.trame() == null ? List.of() : parcours.trame().steps();

        List<Scene> scenes = new ArrayList<>();
        for (int i = 0; i < etapes.size(); i++) {
            EtapeTrame etape = etapes.get(i);
            SceneMiseEnScene miseEnScene = parcours.miseEnScene() == null ? null
                    : parcours.miseEnScene().scenes().get(etape.id());
            String texte = parcours.ecriture() == null ? null : parcours.ecriture().texts().get(etape.id());
            scenes.add(scene(i + 1, etape, texte, miseEnScene, personnagesParId));
        }

        return new ConducteurSpectacle(
                texte(conte.getTitre()),
                texte(conte.getThematique()),
                ages(conte.getAgeMin(), conte.getAgeMax()),
                joindre(parcours.ville(), parcours.region(), conte.getPays()),
                espace(parcours.espaceRepresentation()),
                FORMAT_DATE.format(dateEdition),
                personnages.stream().map(p -> new Personnage(texte(p.name()), texte(p.role()))).toList(),
                scenes);
    }

    private Scene scene(int numero, EtapeTrame etape, String texteHtml, SceneMiseEnScene miseEnScene,
                        Map<String, PersonnageUnivers> personnagesParId) {
        if (miseEnScene == null) {
            return new Scene(numero, texte(etape.title()), texte(etape.summary()), html(texteHtml),
                    List.of(), null, List.of(), List.of(), List.of());
        }
        // Un personnage retire de l'univers apres la mise en scene n'apparait plus
        List<PersonnageScene> presents = miseEnScene.characters().stream()
                .filter(p -> personnagesParId.containsKey(p.characterId()))
                .toList();
        List<String> decors = miseEnScene.props().stream().filter(d -> !d.isBlank()).toList();

        return new Scene(
                numero,
                texte(etape.title()),
                texte(etape.summary()),
                html(texteHtml),
                presents.stream().map(p -> presence(p, personnagesParId.get(p.characterId()))).toList(),
                texte(miseEnScene.intention()),
                decors,
                sons(miseEnScene.sounds()),
                liste(miseEnScene.dances()).stream()
                        .map(d -> danse(d, presents, decors, personnagesParId))
                        .toList());
    }

    private Presence presence(PersonnageScene presence, PersonnageUnivers personnage) {
        String entree = presence.entrance() == PersonnageScene.Entree.onStage
                ? "Sur scène dès le début"
                : precise("Entre pendant la scène", presence.entranceMoment());
        String sortie = presence.exit() == PersonnageScene.Sortie.stays
                ? "Reste jusqu'à la fin"
                : precise("Sort pendant la scène", presence.exitMoment());
        return new Presence(nom(personnage), entree, sortie);
    }

    private Danse danse(DanseScene danse, List<PersonnageScene> presents, List<String> decors,
                        Map<String, PersonnageUnivers> personnagesParId) {
        List<String> personnagesPresents = presents.stream()
                .map(PersonnageScene::characterId)
                .toList();
        // Seuls les elements encore presents dans la scene figurent sur le plan
        List<PlacementScene> places = liste(danse.placements()).stream()
                .filter(p -> p.kind() == PlacementScene.TypeElement.character
                        ? personnagesPresents.contains(p.ref())
                        : decors.contains(p.ref()))
                .toList();

        List<ElementPlateau> plateau = new ArrayList<>();
        int numeroDecor = 0;
        for (PlacementScene place : places) {
            boolean estPersonnage = place.kind() == PlacementScene.TypeElement.character;
            String libelle = estPersonnage ? nom(personnagesParId.get(place.ref())) : place.ref();
            String repere = estPersonnage ? initiales(libelle) : String.valueOf(++numeroDecor);
            String couleur = estPersonnage
                    ? COULEURS_PERSONNAGES.get(personnagesPresents.indexOf(place.ref()) % COULEURS_PERSONNAGES.size())
                    : null;
            plateau.add(new ElementPlateau(libelle, estPersonnage, place.x(), place.y(),
                    zone(place.x(), place.y()), repere, couleur));
        }

        return new Danse(texte(danse.name()), moment(danse.moment()), style(danse.style()),
                sons(danse.sounds()), plateau);
    }

    private List<Son> sons(List<SonScene> sons) {
        return liste(sons).stream()
                .map(son -> new Son(texte(son.title()),
                        son.kind() == SonScene.TypeSon.music ? "Musique" : "Bruitage",
                        moment(son.moment())))
                .toList();
    }

    /** Une des 9 zones du plateau, avec le meme decoupage que le front (zoneOf). */
    static String zone(double x, double y) {
        String rangee = y < 100.0 / 3 ? "back" : y < LIGNE_RIDEAU ? "middle" : "front";
        String colonne = x < 100.0 / 3 ? "left" : x < 200.0 / 3 ? "center" : "right";
        return switch (rangee + "-" + colonne) {
            case "back-left" -> "Fond, côté jardin";
            case "back-center" -> "Fond, au centre";
            case "back-right" -> "Fond, côté cour";
            case "middle-left" -> "Milieu, côté jardin";
            case "middle-center" -> "Centre de la scène";
            case "middle-right" -> "Milieu, côté cour";
            case "front-left" -> "Avant-scène, côté jardin";
            case "front-center" -> "Avant-scène, au centre";
            default -> "Avant-scène, côté cour";
        };
    }

    private static String moment(MomentScene moment) {
        if (moment == null) {
            return "Toute la scène";
        }
        return switch (moment) {
            case beginning -> "Début";
            case middle -> "Milieu";
            case end -> "Fin";
        };
    }

    private static String style(DanseScene.Style style) {
        return switch (style) {
            case traditional -> "Traditionnel";
            case classical -> "Classique";
            case contemporary -> "Contemporain";
        };
    }

    private static String espace(EspaceRepresentation espace) {
        if (espace == null) {
            return null;
        }
        return switch (espace) {
            case salle_spectacle -> "Salle de spectacle";
            case gymnase -> "Gymnase";
            case classe -> "Classe";
            case exterieur -> "Extérieur";
        };
    }

    private static String ages(Integer min, Integer max) {
        if (min == null || max == null) {
            return null;
        }
        return min + " à " + max + " ans";
    }

    private static String nom(PersonnageUnivers personnage) {
        String nom = texte(personnage.name());
        return nom == null ? "Personnage sans nom" : nom;
    }

    private static String initiales(String nom) {
        String initiales = Arrays.stream(nom.trim().split("\\s+"))
                .filter(mot -> !mot.isEmpty())
                .limit(2)
                .map(mot -> mot.substring(0, 1).toUpperCase(Locale.FRENCH))
                .collect(Collectors.joining());
        return initiales.isEmpty() ? "?" : initiales;
    }

    private static String precise(String libelle, String moment) {
        String precision = texte(moment);
        return precision == null ? libelle : libelle + " : " + precision;
    }

    private static String html(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String propre = Jsoup.clean(html, TEXTE_AUTORISE);
        return Jsoup.parse(propre).text().isBlank() ? null : propre;
    }

    private static String joindre(String... parties) {
        String lieu = Stream.of(parties)
                .map(ConducteurSpectacleService::texte)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return lieu.isEmpty() ? null : lieu;
    }

    /** Texte saisi, ou null s'il est vide : le gabarit affiche alors "Non renseigne". */
    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static <T> List<T> liste(List<T> valeurs) {
        return valeurs == null ? List.of() : valeurs;
    }
}
