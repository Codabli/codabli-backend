package com.codabli.service;

import com.codabli.dto.AdresseLivraisonRequest;
import com.codabli.dto.AdresseLivraisonResponse;
import com.codabli.entity.AdresseLivraison;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.AdresseLivraisonRepository;
import com.codabli.repository.UtilisateurRepository;
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
    private final UtilisateurRepository utilisateurRepository;

    public AdresseLivraisonService(AdresseLivraisonRepository adresseLivraisonRepository,
                                   UtilisateurRepository utilisateurRepository) {
        this.adresseLivraisonRepository = adresseLivraisonRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdresseLivraisonResponse> lister(Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        return adresseLivraisonRepository.findByUtilisateurId(utilisateur.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // CREER
    // ────────────────────────────────────────────────────────────────

    public AdresseLivraisonResponse creer(AdresseLivraisonRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);

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
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        AdresseLivraison adresse = adresseLivraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adresse non trouvee avec l'ID: " + id));

        // Verification proprietaire
        if (!adresse.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new SecurityException("Acces interdit a cette adresse");
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
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        AdresseLivraison adresse = adresseLivraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adresse non trouvee avec l'ID: " + id));

        if (!adresse.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new SecurityException("Acces interdit a cette adresse");
        }

        adresseLivraisonRepository.delete(adresse);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private void resetParDefaut(UUID utilisateurId) {
        adresseLivraisonRepository.findByUtilisateurId(utilisateurId)
                .forEach(a -> {
                    if (a.isParDefaut()) {
                        a.setParDefaut(false);
                        adresseLivraisonRepository.save(a);
                    }
                });
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
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
