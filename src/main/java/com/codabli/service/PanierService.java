package com.codabli.service;

import com.codabli.dto.AjoutPanierRequest;
import com.codabli.dto.LignePanierResponse;
import com.codabli.dto.ModifQuantiteRequest;
import com.codabli.dto.PanierResponse;
import com.codabli.entity.LignePanier;
import com.codabli.entity.Panier;
import com.codabli.entity.Produit;
import com.codabli.entity.Utilisateur;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.PanierRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service metier pour le panier.
 * <p>
 * Securite :
 * - Toutes les operations sont authentifiees.
 * - L'utilisateur ne peut acceder qu'a son propre panier (verification via
 * JWT).
 */
@Service
@Transactional
public class PanierService {

    private final PanierRepository panierRepository;

    private final ProduitService produitService;
    private final UtilisateurService utilisateurService;

    public PanierService(PanierRepository panierRepository,
                         ProduitService produitService,
                         UtilisateurService utilisateurService) {
        this.panierRepository = panierRepository;
        this.produitService = produitService;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // CONSULTER LE PANIER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PanierResponse getPanier(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);
        return toResponse(panier);
    }

    // ────────────────────────────────────────────────────────────────
    // AJOUTER UN PRODUIT AU PANIER
    // ────────────────────────────────────────────────────────────────

    public PanierResponse ajouterProduit(AjoutPanierRequest request, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        Produit produit = produitService.findById(request.produitId());

        if (!produit.isActif()) {
            throw new IllegalArgumentException("Ce produit n'est plus disponible");
        }

        // Verifier si le produit est deja dans le panier
        Optional<LignePanier> ligneExistante = panier.getLignes().stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst();

        if (ligneExistante.isPresent()) {
            // Incrementer la quantite
            LignePanier ligne = ligneExistante.get();
            ligne.setQuantite(ligne.getQuantite() + request.quantite());
        } else {
            // Nouvelle ligne
            LignePanier nouvelleLigne = LignePanier.builder()
                    .panier(panier)
                    .produit(produit)
                    .quantite(request.quantite())
                    .build();
            panier.getLignes().add(nouvelleLigne);
        }

        Panier saved = panierRepository.save(panier);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER LA QUANTITE D'UNE LIGNE
    // ────────────────────────────────────────────────────────────────

    public PanierResponse modifierQuantite(UUID ligneId, ModifQuantiteRequest request, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        LignePanier ligne = panier.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_PANIER_LIGNE,
                        "Ligne de panier non trouvee avec l'ID: " + ligneId));

        ligne.setQuantite(request.quantite());

        Panier saved = panierRepository.save(panier);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER UNE LIGNE
    // ────────────────────────────────────────────────────────────────

    public PanierResponse supprimerLigne(UUID ligneId, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        boolean removed = panier.getLignes().removeIf(l -> l.getId().equals(ligneId));
        if (!removed) {
            throw new ResourceNotFoundException(ErrorCode.UNKNOWN_PANIER_LIGNE,
                    "Ligne de panier non trouvee avec l'ID: " + ligneId);
        }

        Panier saved = panierRepository.save(panier);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // VIDER LE PANIER
    // ────────────────────────────────────────────────────────────────

    public void viderPanier(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);
        panier.getLignes().clear();
        panierRepository.save(panier);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES (package-private pour CommandeService)
    // ────────────────────────────────────────────────────────────────

    Panier getOrCreatePanier(Utilisateur utilisateur) {
        return panierRepository.findByUtilisateurId(utilisateur.getId())
                .orElseGet(() -> {
                    Panier nouveauPanier = Panier.builder()
                            .utilisateur(utilisateur)
                            .build();
                    return panierRepository.save(nouveauPanier);
                });
    }

    private PanierResponse toResponse(Panier panier) {
        List<LignePanierResponse> lignes = panier.getLignes().stream()
                .map(this::toLigneResponse)
                .toList();

        BigDecimal total = lignes.stream()
                .map(LignePanierResponse::sousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int nombreArticles = panier.getLignes().stream()
                .mapToInt(LignePanier::getQuantite)
                .sum();

        return new PanierResponse(
                panier.getId(),
                lignes,
                total,
                nombreArticles,
                panier.getDateCreation(),
                panier.getDateMiseAJour());
    }

    private LignePanierResponse toLigneResponse(LignePanier ligne) {
        BigDecimal sousTotal = ligne.getProduit().getPrix()
                .multiply(BigDecimal.valueOf(ligne.getQuantite()));

        return new LignePanierResponse(
                ligne.getId(),
                ligne.getProduit().getId(),
                ligne.getProduit().getNom(),
                ligne.getProduit().getImageUrl(),
                ligne.getProduit().getPrix(),
                ligne.getQuantite(),
                sousTotal);
    }
}
