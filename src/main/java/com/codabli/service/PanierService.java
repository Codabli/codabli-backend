package com.codabli.service;

import com.codabli.dto.AjoutPanierRequest;
import com.codabli.dto.LignePanierResponse;
import com.codabli.dto.ModifQuantiteRequest;
import com.codabli.dto.PanierResponse;
import com.codabli.entity.LignePanier;
import com.codabli.entity.Panier;
import com.codabli.entity.Produit;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.PanierRepository;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour le panier.
 *
 * Securite :
 * - Toutes les operations sont authentifiees.
 * - L'utilisateur ne peut acceder qu'a son propre panier (verification via
 * JWT).
 */
@Service
@Transactional
public class PanierService {

    private final PanierRepository panierRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    public PanierService(PanierRepository panierRepository,
            ProduitRepository produitRepository,
            UtilisateurRepository utilisateurRepository) {
        this.panierRepository = panierRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // CONSULTER LE PANIER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PanierResponse getPanier(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);
        return toResponse(panier);
    }

    // ────────────────────────────────────────────────────────────────
    // AJOUTER UN PRODUIT AU PANIER
    // ────────────────────────────────────────────────────────────────

    public PanierResponse ajouterProduit(AjoutPanierRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        Produit produit = produitRepository.findById(request.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouve avec l'ID: " + request.getProduitId()));

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
            ligne.setQuantite(ligne.getQuantite() + request.getQuantite());
        } else {
            // Nouvelle ligne
            LignePanier nouvelleLigne = LignePanier.builder()
                    .panier(panier)
                    .produit(produit)
                    .quantite(request.getQuantite())
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
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        LignePanier ligne = panier.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ligne de panier non trouvee avec l'ID: " + ligneId));

        ligne.setQuantite(request.getQuantite());

        Panier saved = panierRepository.save(panier);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER UNE LIGNE
    // ────────────────────────────────────────────────────────────────

    public PanierResponse supprimerLigne(UUID ligneId, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Panier panier = getOrCreatePanier(utilisateur);

        boolean removed = panier.getLignes().removeIf(l -> l.getId().equals(ligneId));
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Ligne de panier non trouvee avec l'ID: " + ligneId);
        }

        Panier saved = panierRepository.save(panier);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // VIDER LE PANIER
    // ────────────────────────────────────────────────────────────────

    public void viderPanier(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
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

    Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    private PanierResponse toResponse(Panier panier) {
        List<LignePanierResponse> lignes = panier.getLignes().stream()
                .map(this::toLigneResponse)
                .collect(Collectors.toList());

        BigDecimal total = lignes.stream()
                .map(LignePanierResponse::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int nombreArticles = panier.getLignes().stream()
                .mapToInt(LignePanier::getQuantite)
                .sum();

        return PanierResponse.builder()
                .id(panier.getId())
                .lignes(lignes)
                .total(total)
                .nombreArticles(nombreArticles)
                .dateCreation(panier.getDateCreation())
                .dateMiseAJour(panier.getDateMiseAJour())
                .build();
    }

    private LignePanierResponse toLigneResponse(LignePanier ligne) {
        BigDecimal sousTotal = ligne.getProduit().getPrix()
                .multiply(BigDecimal.valueOf(ligne.getQuantite()));

        return LignePanierResponse.builder()
                .id(ligne.getId())
                .produitId(ligne.getProduit().getId())
                .produitNom(ligne.getProduit().getNom())
                .produitImageUrl(ligne.getProduit().getImageUrl())
                .produitPrix(ligne.getProduit().getPrix())
                .quantite(ligne.getQuantite())
                .sousTotal(sousTotal)
                .build();
    }
}
