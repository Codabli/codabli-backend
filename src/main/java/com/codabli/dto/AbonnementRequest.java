package com.codabli.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO d'entree pour souscrire ou changer d'offre d'abonnement (ABO-02/03).
 */

public record AbonnementRequest(@NotNull(message = "L'offre est obligatoire") UUID offreId) {

}
