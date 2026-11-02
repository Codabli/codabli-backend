package com.codabli.dto.parcours;

/**
 * Contenu d'une etape du parcours, enregistre tel quel dans la colonne JSON
 * de l'etape.
 */
public sealed interface ContenuEtapeParcours permits DecouverteParcours, RessourcesParcours,
        RessourcesClasseParcours, UniversParcours, TrameParcours, EcritureParcours, MiseEnSceneParcours {

    /** Taille maximale d'un identifiant genere par le front ou d'une ressource du catalogue. */
    int ID_MAX = 100;
}
