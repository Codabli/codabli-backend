package com.codabli.dto;

import com.codabli.dto.parcours.DecouverteParcours;
import com.codabli.dto.parcours.EcritureParcours;
import com.codabli.dto.parcours.MiseEnSceneParcours;
import com.codabli.dto.parcours.RessourcesClasseParcours;
import com.codabli.dto.parcours.RessourcesParcours;
import com.codabli.dto.parcours.TrameParcours;
import com.codabli.dto.parcours.UniversParcours;
import com.codabli.entity.enums.EspaceRepresentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Etat du parcours "Creer mon conte danse" d'un conte. Une etape jamais
 * enregistree vaut null.
 */
public record ParcoursConteResponse(
        UUID conteDanseId,
        String region,
        String ville,
        EspaceRepresentation espaceRepresentation,
        List<String> ingredientsSecrets,
        DecouverteParcours decouverte,
        RessourcesParcours ressources,
        RessourcesClasseParcours ressourcesClasse,
        UniversParcours univers,
        TrameParcours trame,
        EcritureParcours ecriture,
        MiseEnSceneParcours miseEnScene,
        OffsetDateTime dateModification) {
}
