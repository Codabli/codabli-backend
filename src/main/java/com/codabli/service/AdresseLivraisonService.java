package com.codabli.service;

import com.codabli.dto.AdresseLivraisonRequest;
import com.codabli.dto.AdresseLivraisonResponse;
import com.codabli.entity.AdresseLivraison;
import com.codabli.entity.Utilisateur;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.AdresseLivraisonRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les adresses de livraison.
 * <p>
 * Securite :
 * - Toutes les operations sont authentifiees.
 * - L'utilisateur ne peut acceder qu'a ses propres adresses (verification via
 * JWT).
 */
@Service
@Transactional
public class AdresseLivraisonService {

    private final AdresseLivraisonRepository adresseLivraisonRepository;

    private final UtilisateurService utilisateurService;

    public AdresseLivraisonService(AdresseLivraisonRepository adresseLivraisonRepository, UtilisateurService utilisateurService) {
        this.adresseLivraisonRepository = adresseLivraisonRepository;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdresseLivraisonResponse> lister(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        return adresseLivraisonRepository.findByUtilisateurId(utilisateur.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // CREER
    // ────────────────────────────────────────────────────────────────

    public AdresseLivraisonResponse creer(AdresseLivraisonRequest request, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);

        // Si cette adresse est par defaut, retirer le flag des autres
        if (request.parDefaut() != null && request.parDefaut()) {
            resetParDefaut(utilisateur.getId());
        }

        AdresseLivraison adresse = AdresseLivraison.builder()
                .utilisateur(utilisateur)
                .nom(request.nom())
                .adresse(request.adresse())
                .codePostal(request.codePostal())
                .ville(request.ville())
                .telephone(request.telephone())
                .parDefaut(request.parDefaut() != null && request.parDefaut())
                .build();

        AdresseLivraison saved = adresseLivraisonRepository.save(adresse);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER
    // ────────────────────────────────────────────────────────────────

    public AdresseLivraisonResponse modifier(UUID id, AdresseLivraisonRequest request, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        AdresseLivraison adresse = findById(id);

        // Verification proprietaire
        if (!adresse.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Acces interdit a cette adresse");
        }

        if (request.parDefaut() != null && request.parDefaut()) {
            resetParDefaut(utilisateur.getId());
        }

        adresse.setNom(request.nom());
        adresse.setAdresse(request.adresse());
        adresse.setCodePostal(request.codePostal());
        adresse.setVille(request.ville());
        adresse.setTelephone(request.telephone());
        if (request.parDefaut() != null) {
            adresse.setParDefaut(request.parDefaut());
        }

        AdresseLivraison saved = adresseLivraisonRepository.save(adresse);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        AdresseLivraison adresse = findById(id);

        if (!adresse.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Acces interdit a cette adresse");
        }

        adresseLivraisonRepository.delete(adresse);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    AdresseLivraison findById(UUID id) {
        return adresseLivraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_ADRESSE,
                        "Adresse non trouvee avec l'ID: " + id));
    }


    private void resetParDefaut(UUID utilisateurId) {
        adresseLivraisonRepository.findByUtilisateurId(utilisateurId)
                .forEach(a -> {
                    if (a.isParDefaut()) {
                        a.setParDefaut(false);
                        adresseLivraisonRepository.save(a);
                    }
                });
    }

    private AdresseLivraisonResponse toResponse(AdresseLivraison adresse) {
        return new AdresseLivraisonResponse(
                adresse.getId(),
                adresse.getNom(),
                adresse.getAdresse(),
                adresse.getCodePostal(),
                adresse.getVille(),
                adresse.getTelephone(),
                adresse.isParDefaut());
    }
}
