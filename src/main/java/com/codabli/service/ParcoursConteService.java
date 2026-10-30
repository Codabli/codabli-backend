package com.codabli.service;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.dto.ParcoursContexteRequest;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ParcoursConte;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.EtapeParcours;
import com.codabli.entity.enums.StatutConte;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.ParcoursConteRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parcours "Creer mon conte danse" : enregistrement, etape par etape, du
 * travail de l'enseignant sur un conte danse.
 * <p>
 * Lecture : auteur du conte ou admin/moderateur.
 * Ecriture : idem, et seulement tant que le conte est en brouillon ou refuse
 * (un conte en moderation ou publie ne se retravaille plus).
 */
@Service
@Transactional
public class ParcoursConteService {

    private final ParcoursConteRepository parcoursRepository;
    private final ConteDanseService conteDanseService;
    private final UtilisateurService utilisateurService;

    public ParcoursConteService(ParcoursConteRepository parcoursRepository,
                                ConteDanseService conteDanseService,
                                UtilisateurService utilisateurService) {
        this.parcoursRepository = parcoursRepository;
        this.conteDanseService = conteDanseService;
        this.utilisateurService = utilisateurService;
    }

    @Transactional(readOnly = true)
    public ParcoursConteResponse lire(UUID conteDanseId, Jwt jwt) {
        verifierAcces(conteDanseService.findById(conteDanseId), jwt);
        return parcoursRepository.findByConteDanseId(conteDanseId)
                .map(this::toResponse)
                .orElseGet(() -> parcoursVide(conteDanseId));
    }

    public ParcoursConteResponse enregistrerContexte(UUID conteDanseId, ParcoursContexteRequest request, Jwt jwt) {
        ParcoursConte parcours = parcoursModifiable(conteDanseId, jwt);
        parcours.setRegion(request.region().trim());
        parcours.setVille(request.ville() == null || request.ville().isBlank() ? null : request.ville().trim());
        parcours.setEspaceRepresentation(request.espaceRepresentation());
        parcours.setIngredientsSecrets(new ArrayList<>(request.ingredientsSecrets()));
        return toResponse(parcoursRepository.save(parcours));
    }

    public ParcoursConteResponse enregistrerEtape(UUID conteDanseId, String chemin, Map<String, Object> contenu,
                                                  Jwt jwt) {
        EtapeParcours etape = EtapeParcours.depuisChemin(chemin);
        ParcoursConte parcours = parcoursModifiable(conteDanseId, jwt);
        parcours.setEtape(etape, contenu);
        return toResponse(parcoursRepository.save(parcours));
    }

    private ParcoursConte parcoursModifiable(UUID conteDanseId, Jwt jwt) {
        ConteDanse conte = conteDanseService.findById(conteDanseId);
        verifierAcces(conte, jwt);
        if (conte.getStatut() != StatutConte.brouillon && conte.getStatut() != StatutConte.refuse) {
            throw new BusinessException(ErrorCode.PARCOURS_NON_MODIFIABLE,
                    "Le parcours d'un conte en moderation ou publie ne peut plus etre modifie");
        }
        return parcoursRepository.findByConteDanseId(conteDanseId).orElseGet(() -> {
            ParcoursConte nouveau = new ParcoursConte();
            nouveau.setConteDanseId(conteDanseId);
            return nouveau;
        });
    }

    private void verifierAcces(ConteDanse conte, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);

        boolean estAuteur = conte.getCreateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'au parcours de vos propres contes danses");
        }
    }

    private ParcoursConteResponse parcoursVide(UUID conteDanseId) {
        return new ParcoursConteResponse(conteDanseId, null, null, null, List.of(),
                null, null, null, null, null, null, null, null);
    }

    private ParcoursConteResponse toResponse(ParcoursConte parcours) {
        return new ParcoursConteResponse(
                parcours.getConteDanseId(),
                parcours.getRegion(),
                parcours.getVille(),
                parcours.getEspaceRepresentation(),
                List.copyOf(parcours.getIngredientsSecrets()),
                parcours.getDecouverte(),
                parcours.getRessources(),
                parcours.getRessourcesClasse(),
                parcours.getUnivers(),
                parcours.getTrame(),
                parcours.getEcriture(),
                parcours.getMiseEnScene(),
                parcours.getDateModification());
    }
}
