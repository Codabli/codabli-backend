package com.codabli.service;

import com.codabli.dto.EvaluationPedagogiqueRequest;
import com.codabli.dto.EvaluationPedagogiqueResponse;
import com.codabli.entity.Classe;
import com.codabli.entity.EvaluationPedagogique;
import com.codabli.entity.Utilisateur;
import com.codabli.repository.EvaluationPedagogiqueRepository;
import com.codabli.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service metier pour les evaluations pedagogiques (CDC PRO-05).
 * <p>
 * Securite : seul l'enseignant auteur de l'evaluation (ou un
 * admin/super_admin) peut la consulter/modifier/supprimer.
 */
@Service
@Transactional
public class EvaluationPedagogiqueService {

    private final EvaluationPedagogiqueRepository evaluationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityManager entityManager;

    public EvaluationPedagogiqueService(EvaluationPedagogiqueRepository evaluationRepository,
                                        UtilisateurRepository utilisateurRepository,
                                        EntityManager entityManager) {
        this.evaluationRepository = evaluationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entityManager = entityManager;
    }

    public EvaluationPedagogiqueResponse creer(EvaluationPedagogiqueRequest request, Jwt jwt) {
        Utilisateur enseignant = getUtilisateurFromJwt(jwt);

        EvaluationPedagogique evaluation = EvaluationPedagogique.builder()
                .enseignant(enseignant)
                .titre(request.titre())
                .competencesEvaluees(request.competencesEvaluees())
                .observations(request.observations())
                .bilan(request.bilan())
                .dateEvaluation(request.dateEvaluation())
                .fichierExportUrl(request.fichierExportUrl())
                .build();

        if (request.classeId() != null) {
            evaluation.setClasse(entityManager.getReference(Classe.class, request.classeId()));
        }
        if (request.eleveId() != null) {
            evaluation.setEleve(entityManager.getReference(Utilisateur.class, request.eleveId()));
        }

        return toResponse(evaluationRepository.save(evaluation));
    }

    @Transactional(readOnly = true)
    public List<EvaluationPedagogiqueResponse> mesEvaluations(Jwt jwt) {
        Utilisateur enseignant = getUtilisateurFromJwt(jwt);
        return evaluationRepository.findByEnseignantIdOrderByDateEvaluationDesc(enseignant.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationPedagogiqueResponse getById(UUID id, Jwt jwt) {
        return toResponse(getAvecDroit(id, jwt));
    }

    public EvaluationPedagogiqueResponse modifier(UUID id, EvaluationPedagogiqueRequest request, Jwt jwt) {
        EvaluationPedagogique evaluation = getAvecDroit(id, jwt);

        evaluation.setTitre(request.titre());
        evaluation.setCompetencesEvaluees(request.competencesEvaluees());
        evaluation.setObservations(request.observations());
        evaluation.setBilan(request.bilan());
        evaluation.setDateEvaluation(request.dateEvaluation());
        evaluation.setFichierExportUrl(request.fichierExportUrl());

        if (request.classeId() != null) {
            evaluation.setClasse(entityManager.getReference(Classe.class, request.classeId()));
        }
        if (request.eleveId() != null) {
            evaluation.setEleve(entityManager.getReference(Utilisateur.class, request.eleveId()));
        }

        return toResponse(evaluationRepository.save(evaluation));
    }

    public void supprimer(UUID id, Jwt jwt) {
        evaluationRepository.delete(getAvecDroit(id, jwt));
    }

    private EvaluationPedagogique getAvecDroit(UUID id, Jwt jwt) {
        Utilisateur utilisateur = getUtilisateurFromJwt(jwt);
        Collection<String> roles = extractRoles(jwt);

        EvaluationPedagogique evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation pedagogique non trouvee avec l'ID: " + id));

        boolean estAuteur = evaluation.getEnseignant().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez gerer que vos propres evaluations pedagogiques");
        }

        return evaluation;
    }

    private Utilisateur getUtilisateurFromJwt(Jwt jwt) {
        return utilisateurRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouve pour keycloakId: " + jwt.getSubject()));
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return java.util.Collections.emptyList();
        }
        return (Collection<String>) realmAccess.get("roles");
    }

    private EvaluationPedagogiqueResponse toResponse(EvaluationPedagogique evaluation) {
        return new EvaluationPedagogiqueResponse(
                evaluation.getId(),
                evaluation.getEnseignant().getId(),
                evaluation.getEnseignant().getPrenom() + " " + evaluation.getEnseignant().getNom(),
                evaluation.getClasse() != null ? evaluation.getClasse().getId() : null,
                evaluation.getEleve() != null ? evaluation.getEleve().getId() : null,
                evaluation.getEleve() != null ? evaluation.getEleve().getPrenom() + " " + evaluation.getEleve().getNom() : null,
                evaluation.getTitre(),
                evaluation.getCompetencesEvaluees(),
                evaluation.getObservations(),
                evaluation.getBilan(),
                evaluation.getDateEvaluation(),
                evaluation.getFichierExportUrl(),
                evaluation.getDateCreation(),
                evaluation.getDateMiseAJour());
    }
}
