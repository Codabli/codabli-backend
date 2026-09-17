package com.codabli.dto;

import com.codabli.entity.enums.StatutCommande;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour une commande complete.
 */
public record CommandeResponse(
        UUID id,
        StatutCommande statut,
        BigDecimal sousTotal,
        BigDecimal fraisLivraison,
        BigDecimal total,
        OffsetDateTime dateCommande,
        String referencePaiement,
        AdresseLivraisonResponse adresseLivraison,
        List<LigneCommandeResponse> lignes) {
}
