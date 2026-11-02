package com.codabli.service;

import java.util.List;

/**
 * Contenu du conducteur du spectacle (PDF de l'ecran 8), pret a afficher :
 * libelles en francais, elements disparus du parcours deja retires. Un champ
 * texte null est affiche "Non renseigne" par le gabarit.
 *
 * @param ages         tranche d'age des acteurs, ex. "6 a 8 ans"
 * @param lieu         ville, region et pays, ex. "Tours, Centre-Val de Loire, France"
 * @param dateEdition  date de generation, ex. "2 novembre 2026"
 * @param distribution personnages de l'univers (ecran 5)
 * @param scenes       une scene par etape de la trame (ecran 6), dans l'ordre
 */
public record ConducteurSpectacle(
        String titre,
        String theme,
        String ages,
        String lieu,
        String espace,
        String dateEdition,
        List<Personnage> distribution,
        List<Scene> scenes) {

    public record Personnage(String nom, String role) {
    }

    /**
     * @param texteHtml texte de l'etape (ecran 7), HTML nettoye
     * @param presences personnages sur scene (ecran 8.1)
     */
    public record Scene(
            int numero,
            String titre,
            String resume,
            String texteHtml,
            List<Presence> presences,
            String intention,
            List<String> decors,
            List<Son> sons,
            List<Danse> danses) {
    }

    /** Personnage sur scene : entree et sortie, avec leur moment precis s'il est renseigne. */
    public record Presence(String personnage, String entree, String sortie) {
    }

    /** Musique ou bruitage ; moment "Toute la scene" s'il n'est pas precise. */
    public record Son(String titre, String type, String moment) {
    }

    /**
     * @param musiques ambiance musicale de la danse
     * @param plateau  placement au debut de la danse ; vide si rien n'est place
     */
    public record Danse(String nom, String moment, String style, List<Son> musiques, List<ElementPlateau> plateau) {
    }

    /**
     * Personnage ou decor place sur le plateau.
     *
     * @param x     de gauche (jardin) a droite (cour) vu du public, en pourcentage
     * @param y     du fond de scene vers le public, en pourcentage
     * @param zone  une des 9 zones du plateau, ex. "Milieu, cote jardin"
     * @param repere initiales d'un personnage ou numero d'un decor, affiche sur le plan
     */
    public record ElementPlateau(String libelle, boolean personnage, double x, double y, String zone, String repere) {
    }
}
