package com.codabli.dto;

import com.codabli.entity.enums.StatutCommande;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entree pour changer le statut d'une commande (admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangerStatutCommandeRequest {

    @NotNull(message = "Le statut est obligatoire")
    private StatutCommande statut;
}
