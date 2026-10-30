package com.codabli.entity.enums;

import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;

import java.util.Arrays;

/**
 * Etapes du parcours "Creer mon conte danse" stockees en JSON, une colonne
 * par etape. Le contexte (ecran 1) a ses propres colonnes.
 */
public enum EtapeParcours {
    decouverte("decouverte"),
    ressources("ressources"),
    ressources_classe("ressources-classe"),
    univers("univers"),
    trame("trame"),
    ecriture("ecriture"),
    mise_en_scene("mise-en-scene");

    private final String chemin;

    EtapeParcours(String chemin) {
        this.chemin = chemin;
    }

    public String getChemin() {
        return chemin;
    }

    /** Retrouve l'etape a partir du segment d'URL (ex. "ressources-classe"). */
    public static EtapeParcours depuisChemin(String chemin) {
        return Arrays.stream(values())
                .filter(etape -> etape.chemin.equals(chemin))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ETAPE_PARCOURS_INCONNUE,
                        "Etape de parcours inconnue : " + chemin));
    }
}
