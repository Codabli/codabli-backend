package com.codabli.service;

import com.codabli.dto.ProduitRequest;
import com.codabli.dto.ProduitResponse;
import com.codabli.entity.Produit;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.ProduitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service metier pour les produits.
 * <p>
 * Securite :
 * - Lecture (GET) : publique
 * - Ecriture (POST/PUT/DELETE) : reservee aux admins via @PreAuthorize dans le
 * controller
 */
@Service
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProduitResponse> lister(TypeProduit type, Boolean actif, Pageable pageable) {
        Page<Produit> produits;

        if (type != null && actif != null && actif) {
            produits = produitRepository.findByTypeAndActifTrue(type, pageable);
        } else if (type != null) {
            produits = produitRepository.findByType(type, pageable);
        } else if (actif != null && actif) {
            produits = produitRepository.findByActifTrue(pageable);
        } else {
            produits = produitRepository.findAll(pageable);
        }

        return produits.map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // RECHERCHE — utilisee par RechercheController (REC-01)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProduitResponse> rechercher(String query, Pageable pageable) {
        return produitRepository.rechercherActifs(query, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProduitResponse getById(UUID id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouve avec l'ID: " + id));
        return toResponse(produit);
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — admin only
    // ────────────────────────────────────────────────────────────────

    public ProduitResponse creer(ProduitRequest request) {
        Produit produit = Produit.builder()
                .nom(request.nom())
                .description(request.description())
                .prix(request.prix())
                .imageUrl(request.imageUrl())
                .type(request.type())
                .stock(request.stock())
                .actif(request.actif() != null ? request.actif() : true)
                .build();

        Produit saved = produitRepository.save(produit);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — admin only
    // ────────────────────────────────────────────────────────────────

    public ProduitResponse modifier(UUID id, ProduitRequest request) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouve avec l'ID: " + id));

        produit.setNom(request.nom());
        produit.setDescription(request.description());
        produit.setPrix(request.prix());
        produit.setImageUrl(request.imageUrl());
        produit.setType(request.type());
        produit.setStock(request.stock());
        if (request.actif() != null) {
            produit.setActif(request.actif());
        }

        Produit saved = produitRepository.save(produit);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — admin only
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouve avec l'ID: " + id));
        produitRepository.delete(produit);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private ProduitResponse toResponse(Produit produit) {
        return new ProduitResponse(
                produit.getId(),
                produit.getNom(),
                produit.getDescription(),
                produit.getPrix(),
                produit.getImageUrl(),
                produit.getType(),
                produit.getStock(),
                produit.isActif(),
                produit.getDateCreation(),
                produit.getDateMiseAJour());
    }
}
