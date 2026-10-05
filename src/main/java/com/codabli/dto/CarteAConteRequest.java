package com.codabli.dto;

import com.codabli.entity.enums.TypeCarte;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO d'entrée pour la création d'une carte à conte.
 * <p>
 * Seul le champ "type" est obligatoire (personnage, lieu, objet_magique).
 * Le champ "conteId" est optionnel : une carte peut être créée
 * indépendamment d'un conte, puis rattachée plus tard.
 * <p>
 * Note : le créateur n'est PAS dans le DTO — il est extrait du JWT
 * dans le service (principe de sécurité : ne jamais faire confiance
 * au client pour identifier l'utilisateur courant).
 */
public record CarteAConteRequest(
        @NotNull(message = "Le type de carte est obligatoire")
        TypeCarte type,
        String imageUrl,
        String texteAssocie,
        UUID conteId) {
}
