package com.codabli.dto;

import com.codabli.entity.enums.StatutCommande;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de sortie pour une commande complete.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandeResponse {

    private UUID id;
    private StatutCommande statut;
    private BigDecimal sousTotal;
    private BigDecimal fraisLivraison;
    private BigDecimal total;
    private OffsetDateTime dateCommande;
    private String referencePaiement;
    private AdresseLivraisonResponse adresseLivraison;
    private List<LigneCommandeResponse> lignes;
}
