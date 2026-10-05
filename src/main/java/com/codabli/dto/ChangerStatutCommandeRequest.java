package com.codabli.dto;

import com.codabli.entity.enums.StatutCommande;
import jakarta.validation.constraints.NotNull;

/**
 * DTO d'entree pour changer le statut d'une commande (admin).
 */
public record ChangerStatutCommandeRequest(@NotNull(message = "Le statut est obligatoire") StatutCommande statut) {
}
