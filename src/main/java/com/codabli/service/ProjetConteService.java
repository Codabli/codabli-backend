package com.codabli.service;

import com.codabli.dto.ProjetConteRequest;
import com.codabli.dto.ProjetConteResponse;
import com.codabli.entity.ProjetConte;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.ProjetConteRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service metier des projets de conte danse (parcours "Creer mon conte").
 * <p>
 * Securite :
 * - Le role est verifie par @PreAuthorize dans le controleur.
 * - Un enseignant n'accede qu'a ses propres projets (verification via JWT).
 */
@Service
@Transactional
public class ProjetConteService {

    private final ProjetConteRepository projetConteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ProjetConteService(ProjetConteRepository projetConteRepository,
                              UtilisateurRepository utilisateurRepository) {
        this.projetConteRepository = projetConteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER / CONSULTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjetConteResponse> listerMesProjets(Jwt jwt) {
        Utilisateur enseignant = getUtilisateurFromJwt(jwt);
        return projetConteRepository.findByEnseignantIdOrderByDateModificationDesc(enseignant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetConteResponse consulter(UUID id, Jwt jwt) {
        return toResponse(getProjetDuProprietaire(id, getUtilisateurFromJwt(jwt)));
    }

    // ────────────────────────────────────────────────────────────────
    // CREER / MODIFIER LE CONTEXTE
    // ────────────────────────────────────────────────────────────────

    public ProjetConteResponse creer(ProjetConteRequest request, Jwt jwt) {
        ProjetConte projet = ProjetConte.builder()
                .enseignant(getUtilisateurFromJwt(jwt))
                .build();
        appliquerContexte(projet, request);

        return toResponse(projetConteRepository.save(projet));
    }

    public ProjetConteResponse modifierContexte(UUID id, ProjetConteRequest request, Jwt jwt) {
        ProjetConte projet = getProjetDuProprietaire(id, getUtilisateurFromJwt(jwt));
        appliquerContexte(projet, request);

        return toResponse(projetConteRepository.save(projet));
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private void appliquerContexte(ProjetConte projet, ProjetConteRequest request) {
        projet.setTrancheAge(request.trancheAge());
        projet.setPays(request.pays().trim());
        projet.setRegion(request.region().trim());
        projet.setVille(request.ville() == null || request.ville().isBlank() ? null : request.ville().trim());
        projet.setTheme(request.theme().trim());
        projet.setEspaceRepresentation(request.espaceRepresentation());

        // Collection geree par Hibernate : on la modifie plutot que de la remplacer.
        projet.getIngredientsSecrets().clear();
        projet.getIngredientsSecrets().addAll(normaliserIngredients(request.ingredientsSecrets()));
    }

    /**
     * RG-CMC-02 : mots-cles nettoyes, sans doublon (insensible a la casse), ordre de saisie conserve.
     */
    private List<String> normaliserIngredients(List<String> ingredients) {
        if (ingredients == null) {
            return List.of();
        }

        Map<String, String> uniques = new LinkedHashMap<>();
        for (String ingredient : ingredients) {
            String nettoye = ingredient.trim();
            uniques.putIfAbsent(nettoye.toLowerCase(), nettoye);
        }
        return new ArrayList<>(uniques.values());
    }

    private ProjetConte getProjetDuProprietaire(UUID id, Utilisateur enseignant) {
        ProjetConte projet = projetConteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet de conte non trouve avec l'ID: " + id));

        if (!projet.getEnseignant().getId().equals(enseignant.getId())) {
            throw new AccessDeniedException("Acces interdit a ce projet de conte");
        }
        return projet;
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + keycloakId));
    }

    private ProjetConteResponse toResponse(ProjetConte projet) {
        return new ProjetConteResponse(
                projet.getId(),
                projet.getEnseignant().getId(),
                projet.getTrancheAge(),
                projet.getPays(),
                projet.getRegion(),
                projet.getVille(),
                projet.getTheme(),
                List.copyOf(projet.getIngredientsSecrets()),
                projet.getEspaceRepresentation(),
                projet.getDateCreation(),
                projet.getDateModification());
    }
}
