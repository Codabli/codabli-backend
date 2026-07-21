package com.codabli.service;

import com.codabli.dto.AdresseLivraisonResponse;
import com.codabli.dto.CommandeResponse;
import com.codabli.dto.CreerCommandeRequest;
import com.codabli.dto.LigneCommandeResponse;
import com.codabli.entity.AdresseLivraison;
import com.codabli.entity.Commande;
import com.codabli.entity.LigneCommande;
import com.codabli.entity.LignePanier;
import com.codabli.entity.Panier;
import com.codabli.entity.Produit;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.StatutCommande;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.AdresseLivraisonRepository;
import com.codabli.repository.CommandeRepository;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les commandes.
 *
 * Securite :
 * - Creation / consultation : authentifie, propre a l'utilisateur connecte
 * - Changement de statut : admin uniquement via @PreAuthorize
 */
@Service
@Transactional
public class CommandeService {

    private static final BigDecimal FRAIS_LIVRAISON_DEFAUT = new BigDecimal("5.00");

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final AdresseLivraisonRepository adresseLivraisonRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PanierService panierService;

    public CommandeService(CommandeRepository commandeRepository,
            ProduitRepository produitRepository,
            AdresseLivraisonRepository adresseLivraisonRepository,
            UtilisateurRepository utilisateurRepository,
            PanierService panierService) {
        this.commandeRepository = commandeRepository;
        this.produitRepository = produitRepository;
        this.adresseLivraisonRepository = adresseLivraisonRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.panierService = panierService;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER UNE COMMANDE a partir du panier
    // ────────────────────────────────────────────────────────────────

    public CommandeResponse creerCommande(CreerCommandeRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Panier panier = panierService.getOrCreatePanier(utilisateur);

        // Verifier que le panier n'est pas vide
        if (panier.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Le panier est vide, impossible de creer une commande");
        }

        // Recuperer l'adresse de livraison si fournie
        AdresseLivraison adresseLivraison = null;
        if (request.getAdresseLivraisonId() != null) {
            adresseLivraison = adresseLivraisonRepository.findById(request.getAdresseLivraisonId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Adresse de livraison non trouvee avec l'ID: " + request.getAdresseLivraisonId()));

            // Verifier que l'adresse appartient a l'utilisateur
            if (!adresseLivraison.getUtilisateur().getId().equals(utilisateur.getId())) {
                throw new AccessDeniedException("Cette adresse ne vous appartient pas");
            }
        }

        // Determiner si des produits physiques sont presents
        boolean contientPhysique = panier.getLignes().stream()
                .anyMatch(l -> l.getProduit().getType() == TypeProduit.produit_physique);

        // Calculer les frais de livraison
        BigDecimal fraisLivraison = contientPhysique ? FRAIS_LIVRAISON_DEFAUT : BigDecimal.ZERO;

        // Creer la commande
        Commande commande = Commande.builder()
                .utilisateur(utilisateur)
                .statut(StatutCommande.en_attente)
                .adresseLivraison(adresseLivraison)
                .fraisLivraison(fraisLivraison)
                .build();

        BigDecimal sousTotal = BigDecimal.ZERO;
        List<LigneCommande> lignesCommande = new ArrayList<>();

        for (LignePanier lignePanier : panier.getLignes()) {
            Produit produit = lignePanier.getProduit();

            // Verifier le stock pour les produits physiques
            if (produit.getType() == TypeProduit.produit_physique) {
                if (produit.getStock() == null || produit.getStock() < lignePanier.getQuantite()) {
                    throw new StockInsuffisantException(
                            "Stock insuffisant pour le produit '" + produit.getNom()
                                    + "'. Stock disponible: " + (produit.getStock() != null ? produit.getStock() : 0)
                                    + ", quantite demandee: " + lignePanier.getQuantite());
                }

                // Decrementer le stock
                produit.setStock(produit.getStock() - lignePanier.getQuantite());
                produitRepository.save(produit);
            }

            // Snapshot du prix au moment de l'achat
            BigDecimal prixUnitaire = produit.getPrix();
            BigDecimal sousTotalLigne = prixUnitaire.multiply(BigDecimal.valueOf(lignePanier.getQuantite()));
            sousTotal = sousTotal.add(sousTotalLigne);

            LigneCommande ligneCommande = LigneCommande.builder()
                    .commande(commande)
                    .produit(produit)
                    .nomProduit(produit.getNom())
                    .prixUnitaire(prixUnitaire)
                    .quantite(lignePanier.getQuantite())
                    .build();
            lignesCommande.add(ligneCommande);
        }

        commande.setSousTotal(sousTotal);
        commande.setTotal(sousTotal.add(fraisLivraison));
        commande.setLignes(lignesCommande);

        Commande saved = commandeRepository.save(commande);

        // Vider le panier apres la creation de la commande
        panier.getLignes().clear();
        panierService.getOrCreatePanier(utilisateur);

        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER LES COMMANDES de l'utilisateur connecte
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CommandeResponse> listerCommandes(Jwt jwt, Pageable pageable) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return commandeRepository
                .findByUtilisateurIdOrderByDateCommandeDesc(utilisateur.getId(), pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL D'UNE COMMANDE
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CommandeResponse getCommandeById(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande non trouvee avec l'ID: " + id));

        // Verification : l'utilisateur ne peut voir que ses propres commandes
        // sauf s'il est admin (verifie par @PreAuthorize dans le controller,
        // ici on verifie la propriete)
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null
                && jwt.getClaimAsMap("realm_access").get("roles") instanceof List<?> roles
                && roles.contains("admin");

        if (!commande.getUtilisateur().getId().equals(utilisateur.getId()) && !isAdmin) {
            throw new AccessDeniedException("Vous ne pouvez pas acceder a cette commande");
        }

        return toResponse(commande);
    }

    // ────────────────────────────────────────────────────────────────
    // CHANGER LE STATUT — admin uniquement (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public CommandeResponse changerStatut(UUID id, StatutCommande nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande non trouvee avec l'ID: " + id));

        commande.setStatut(nouveauStatut);
        Commande saved = commandeRepository.save(commande);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    private CommandeResponse toResponse(Commande commande) {
        List<LigneCommandeResponse> lignes = commande.getLignes().stream()
                .map(this::toLigneResponse)
                .collect(Collectors.toList());

        AdresseLivraisonResponse adresseResponse = null;
        if (commande.getAdresseLivraison() != null) {
            AdresseLivraison a = commande.getAdresseLivraison();
            adresseResponse = AdresseLivraisonResponse.builder()
                    .id(a.getId())
                    .nom(a.getNom())
                    .adresse(a.getAdresse())
                    .codePostal(a.getCodePostal())
                    .ville(a.getVille())
                    .telephone(a.getTelephone())
                    .parDefaut(a.isParDefaut())
                    .build();
        }

        return CommandeResponse.builder()
                .id(commande.getId())
                .statut(commande.getStatut())
                .sousTotal(commande.getSousTotal())
                .fraisLivraison(commande.getFraisLivraison())
                .total(commande.getTotal())
                .dateCommande(commande.getDateCommande())
                .referencePaiement(commande.getReferencePaiement())
                .adresseLivraison(adresseResponse)
                .lignes(lignes)
                .build();
    }

    private LigneCommandeResponse toLigneResponse(LigneCommande ligne) {
        BigDecimal sousTotal = ligne.getPrixUnitaire()
                .multiply(BigDecimal.valueOf(ligne.getQuantite()));

        return LigneCommandeResponse.builder()
                .id(ligne.getId())
                .produitId(ligne.getProduit() != null ? ligne.getProduit().getId() : null)
                .nomProduit(ligne.getNomProduit())
                .prixUnitaire(ligne.getPrixUnitaire())
                .quantite(ligne.getQuantite())
                .sousTotal(sousTotal)
                .build();
    }
}
