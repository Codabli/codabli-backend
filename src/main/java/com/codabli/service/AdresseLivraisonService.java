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
import java.util.stream.Collectors;

/**
 * Service metier pour les adresses de livraison.
 *
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
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // CREER
    // ────────────────────────────────────────────────────────────────

    public AdresseLivraisonResponse creer(AdresseLivraisonRequest request, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);

        // Si cette adresse est par defaut, retirer le flag des autres
        if (request.getParDefaut() != null && request.getParDefaut()) {
            resetParDefaut(utilisateur.getId());
        }

        AdresseLivraison adresse = AdresseLivraison.builder()
                .utilisateur(utilisateur)
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .codePostal(request.getCodePostal())
                .ville(request.getVille())
                .telephone(request.getTelephone())
                .parDefaut(request.getParDefaut() != null && request.getParDefaut())
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

        if (request.getParDefaut() != null && request.getParDefaut()) {
            resetParDefaut(utilisateur.getId());
        }

        adresse.setNom(request.getNom());
        adresse.setAdresse(request.getAdresse());
        adresse.setCodePostal(request.getCodePostal());
        adresse.setVille(request.getVille());
        adresse.setTelephone(request.getTelephone());
        if (request.getParDefaut() != null) {
            adresse.setParDefaut(request.getParDefaut());
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
        return AdresseLivraisonResponse.builder()
                .id(adresse.getId())
                .nom(adresse.getNom())
                .adresse(adresse.getAdresse())
                .codePostal(adresse.getCodePostal())
                .ville(adresse.getVille())
                .telephone(adresse.getTelephone())
                .parDefaut(adresse.isParDefaut())
                .build();
    }
}
