package com.codabli.service;

import com.codabli.dto.JournalActiviteResponse;
import com.codabli.entity.JournalActivite;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.JournalActiviteRepository;
import com.codabli.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'historisation des actions sensibles (CDC 8.23, SUP-03).
 *
 * Appele depuis les autres services (ex: suppression/suspension d'un
 * utilisateur) pour tracer qui a fait quoi. La consultation est reservee au
 * super-administrateur (verifie dans le controller).
 */
@Service
@Transactional
public class JournalActiviteService {

    private final JournalActiviteRepository journalActiviteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public JournalActiviteService(JournalActiviteRepository journalActiviteRepository,
            UtilisateurRepository utilisateurRepository) {
        this.journalActiviteRepository = journalActiviteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Enregistre une action sensible. Le jwt peut etre null pour une action
     * declenchee sans contexte d'authentification (traitement systeme).
     */
    public void enregistrer(Jwt jwt, String action, String cible, String details) {
        Utilisateur acteur = null;
        if (jwt != null) {
            acteur = utilisateurRepository.findByKeycloakId(jwt.getSubject()).orElse(null);
        }

        JournalActivite entree = JournalActivite.builder()
                .acteur(acteur)
                .action(action)
                .cible(cible)
                .details(details)
                .build();

        journalActiviteRepository.save(entree);
    }

    @Transactional(readOnly = true)
    public Page<JournalActiviteResponse> lister(Pageable pageable) {
        return journalActiviteRepository.findAllByOrderByDateActionDesc(pageable)
                .map(entree -> JournalActiviteResponse.builder()
                        .id(entree.getId())
                        .acteurId(entree.getActeur() != null ? entree.getActeur().getId() : null)
                        .acteurNom(entree.getActeur() != null
                                ? entree.getActeur().getPrenom() + " " + entree.getActeur().getNom()
                                : null)
                        .action(entree.getAction())
                        .cible(entree.getCible())
                        .details(entree.getDetails())
                        .dateAction(entree.getDateAction())
                        .build());
    }
}
