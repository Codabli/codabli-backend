package com.codabli.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO d'entree pour souscrire ou changer d'offre d'abonnement (ABO-02/03).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbonnementRequest {

    @NotNull(message = "L'offre est obligatoire")
    private UUID offreId;
}
