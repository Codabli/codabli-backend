package com.codabli.service;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.dto.ConteDanseTraductionResponse;
import com.codabli.entity.Classe;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Ecole;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.StatutConte;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.event.ConteDansePublieEvent;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.ConteDanseRepository;
import com.codabli.repository.ConteDanseTraductionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier pour les contes danses.
 * <p>
 * Securite a deux niveaux :
 * 1. Niveau role (grossier) : gere par @PreAuthorize dans le controller
 * 2. Niveau relation (fin) : gere ICI — seul l'auteur ou un admin peut
 * modifier/supprimer
 */
@Service
@Transactional
public class ConteDanseService {

    private final ConteDanseRepository conteDanseRepository;
    private final UtilisateurService utilisateurService;
    private final ConteDanseTraductionRepository traductionRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public ConteDanseService(ConteDanseRepository conteDanseRepository,
                             UtilisateurService utilisateurService,
                             ConteDanseTraductionRepository traductionRepository,
                             EntityManager entityManager,
                             ApplicationEventPublisher eventPublisher) {
        this.conteDanseRepository = conteDanseRepository;
        this.utilisateurService = utilisateurService;
        this.traductionRepository = traductionRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    // ────────────────────────────────────────────────────────────────
    // CREER — utilisateur authentifie (verifie par @PreAuthorize)
    // ────────────────────────────────────────────────────────────────

    public ConteDanseResponse creer(ConteDanseRequest request, Jwt jwt) {
        Utilisateur createur = utilisateurService.getUtilisateurFromJwt(jwt);

        ConteDanse conte = ConteDanse.builder()
                .createur(createur)
                .titre(request.titre())
                .description(request.description())
                .thematique(request.thematique())
                .langueOriginale(request.langueOriginale())
                .couvertureUrl(request.couvertureUrl())
                .pays(request.pays())
                .culture(request.culture())
                .ageMin(request.ageMin())
                .ageMax(request.ageMax())
                .dureeMinutes(request.dureeMinutes())
                .credits(request.credits())
                .fichierTexteUrl(request.fichierTexteUrl())
                .fichierAudioUrl(request.fichierAudioUrl())
                .fichierVideoUrl(request.fichierVideoUrl())
                .build();

        // Rattacher a une ecole si ecoleId est fourni
        if (request.ecoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.ecoleId());
            conte.setEcole(ecole);
        }

        // Rattacher a une classe si classeId est fourni
        if (request.classeId() != null) {
            Classe classe = entityManager.getReference(Classe.class, request.classeId());
            conte.setClasse(classe);
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER — auteur ou admin (verification fine dans le service)
    // ────────────────────────────────────────────────────────────────

    public ConteDanseResponse modifier(UUID id, ConteDanseRequest request, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        ConteDanse conte = findById(id);

        // Verification fine : seul l'auteur ou un admin peut modifier
        verifierAuteurOuAdmin(conte, utilisateur, jwt);

        conte.setTitre(request.titre());
        conte.setDescription(request.description());
        conte.setThematique(request.thematique());
        conte.setCouvertureUrl(request.couvertureUrl());
        conte.setPays(request.pays());
        conte.setCulture(request.culture());
        conte.setAgeMin(request.ageMin());
        conte.setAgeMax(request.ageMax());
        conte.setDureeMinutes(request.dureeMinutes());
        conte.setCredits(request.credits());
        conte.setFichierTexteUrl(request.fichierTexteUrl());
        conte.setFichierAudioUrl(request.fichierAudioUrl());
        conte.setFichierVideoUrl(request.fichierVideoUrl());

        if (request.langueOriginale() != null) {
            conte.setLangueOriginale(request.langueOriginale());
        }

        if (request.ecoleId() != null) {
            Ecole ecole = entityManager.getReference(Ecole.class, request.ecoleId());
            conte.setEcole(ecole);
        }

        if (request.classeId() != null) {
            Classe classe = entityManager.getReference(Classe.class, request.classeId());
            conte.setClasse(classe);
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // WORKFLOW DE MODERATION (RG-01) — soumettre / valider / refuser
    //
    // Cycle : brouillon --soumettre--> en_revision_enseignant
    //         --valider(enseignant)--> en_revision_comite
    //         --valider(comite_lecture)--> publie
    // admin/super_admin ont un droit total : valider() publie directement
    // depuis n'importe quel statut. refuser() est possible a tout moment
    // par un role habilite.
    // ────────────────────────────────────────────────────────────────

    /**
     * L'auteur soumet son brouillon (ou un conte refuse corrige) a la
     * moderation.
     */
    public ConteDanseResponse soumettre(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        ConteDanse conte = findById(id);

        if (!conte.getCreateur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Seul l'auteur peut soumettre son conte a la moderation");
        }
        if (conte.getStatut() != StatutConte.brouillon && conte.getStatut() != StatutConte.refuse) {
            throw new IllegalStateException(
                    "Seul un conte en brouillon ou refuse peut etre soumis a la moderation");
        }

        conte.setStatut(StatutConte.en_revision_enseignant);
        conte.setMotifModeration(null);
        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    /**
     * Fait progresser un conte dans le cycle de validation :
     * - enseignant : en_revision_enseignant -> en_revision_comite (limite a
     * ses propres classes si le conte est rattache a une classe)
     * - comite_lecture : en_revision_comite -> publie
     * - admin/super_admin : droit total, publie directement
     */
    public ConteDanseResponse valider(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        ConteDanse conte = findById(id);

        if (roles.contains("admin") || roles.contains("super_admin")) {
            publier(conte, utilisateur);
        } else if (roles.contains("comite_lecture")) {
            if (conte.getStatut() != StatutConte.en_revision_comite) {
                throw new IllegalStateException(
                        "Ce conte n'est pas en attente de validation du comite de lecture");
            }
            publier(conte, utilisateur);
        } else if (roles.contains("enseignant")) {
            if (conte.getStatut() != StatutConte.en_revision_enseignant) {
                throw new IllegalStateException(
                        "Ce conte n'est pas en attente de validation enseignant");
            }
            if (conte.getClasse() != null
                    && !conte.getClasse().getEnseignant().getId().equals(utilisateur.getId())) {
                throw new AccessDeniedException("Vous ne pouvez valider que les contes de vos classes");
            }
            conte.setValideParEnseignant(utilisateur);
            conte.setStatut(StatutConte.en_revision_comite);
        } else {
            throw new AccessDeniedException("Role non autorise a valider un conte danse");
        }

        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    /**
     * Refuse un conte, quel que soit son statut de revision en cours (MOD-03,
     * le motif est enregistre).
     */
    public ConteDanseResponse refuser(UUID id, String motif, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        ConteDanse conte = findById(id);

        boolean autorise = roles.contains("admin") || roles.contains("super_admin")
                || roles.contains("comite_lecture");
        if (!autorise && roles.contains("enseignant") && conte.getStatut() == StatutConte.en_revision_enseignant
                && (conte.getClasse() == null
                || conte.getClasse().getEnseignant().getId().equals(utilisateur.getId()))) {
            autorise = true;
        }
        if (!autorise) {
            throw new AccessDeniedException("Vous n'etes pas autorise a refuser ce conte danse");
        }

        conte.setStatut(StatutConte.refuse);
        conte.setMotifModeration(motif);
        ConteDanse saved = conteDanseRepository.save(conte);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MES CONTES — l'auteur retrouve ses propres contes, quel que soit
    // leur statut (brouillon, en revision, publie, refuse)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseResponse> mesContes(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        return conteDanseRepository.findByCreateurId(utilisateur.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // FILE D'ATTENTE DE MODERATION — selon le role de l'appelant
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConteDanseResponse> listerEnAttente(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);

        List<ConteDanse> contes;
        if (roles.contains("admin") || roles.contains("super_admin")) {
            contes = conteDanseRepository.findByStatutIn(
                    List.of(StatutConte.en_revision_enseignant, StatutConte.en_revision_comite));
        } else if (roles.contains("comite_lecture")) {
            contes = conteDanseRepository.findByStatut(StatutConte.en_revision_comite);
        } else if (roles.contains("enseignant")) {
            contes = conteDanseRepository.findByStatutAndEnseignant(
                    StatutConte.en_revision_enseignant, utilisateur.getId());
        } else {
            throw new AccessDeniedException("Role non autorise a consulter la file de moderation");
        }

        return contes.stream().map(this::toResponse).toList();
    }

    private void publier(ConteDanse conte, Utilisateur validateur) {
        entityManager.lock(conte, LockModeType.PESSIMISTIC_WRITE);
        conte.setStatut(StatutConte.publie);
        eventPublisher.publishEvent(new ConteDansePublieEvent(conte.getId()));
        conte.setValideParComite(validateur);
        conte.setDatePublication(OffsetDateTime.now());
        conte.setMotifModeration(null);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER — auteur ou admin
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        ConteDanse conte = findById(id);

        verifierAuteurOuAdmin(conte, utilisateur, jwt);
        conteDanseRepository.delete(conte);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — public, uniquement les publies
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> listerPublies(Pageable pageable) {
        return ajouterTraductions(conteDanseRepository
                .findByStatutOrderByDatePublicationDesc(StatutConte.publie, pageable));
    }

    // ────────────────────────────────────────────────────────────────
    // FILTRER — public, CON-03 (age, langue, pays, thematique, acces)
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> filtrerPublies(String langue, String pays, String thematique,
                                                   AccesConte acces, Integer age, Pageable pageable) {
        return ajouterTraductions(conteDanseRepository
                .findAll(filtrerPubliesPar(langue, pays, thematique, acces, age),
                        pageable.getSort().isSorted()
                                ? pageable
                                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                Sort.by(Sort.Direction.DESC, "datePublication"))));
    }

    private Page<ConteDanseResponse> ajouterTraductions(Page<ConteDanse> page) {
        List<UUID> conteIds = page.getContent().stream()
                .map(ConteDanse::getId)
                .toList();
        if (conteIds.isEmpty()) {
            return page.map(this::toResponse);
        }

        Map<UUID, List<ConteDanseTraductionResponse>> traductionsParConte = traductionRepository
                .findByConteIdInAndStatut(conteIds, StatutTraduction.publiee)
                .stream()
                .collect(Collectors.groupingBy(traduction -> traduction.getConte().getId(),
                        Collectors.mapping(this::toTraductionResponse, Collectors.toList())));

        return page.map(conte -> toResponse(conte,
                traductionsParConte.getOrDefault(conte.getId(), List.of())));
    }

    private Specification<ConteDanse> filtrerPubliesPar(String langue, String pays, String thematique,
                                                        AccesConte acces, Integer age) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutConte.publie));
            ajouterFiltreLangue(predicates, root, query, criteriaBuilder, langue);
            ajouterEgaliteIgnoree(predicates, criteriaBuilder, root.get("pays"), pays);

            if (estRenseigne(thematique)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("thematique")),
                        "%" + thematique.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            if (acces != null) {
                predicates.add(criteriaBuilder.equal(root.get("acces"), acces));
            }
            if (age != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("ageMin")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("ageMin"), age)));
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("ageMax")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("ageMax"), age)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void ajouterFiltreLangue(List<Predicate> predicates,
                                     Root<ConteDanse> conte,
                                     CriteriaQuery<?> query,
                                     CriteriaBuilder criteriaBuilder,
                                     String langue) {
        if (!estRenseigne(langue)) {
            return;
        }

        String langueNormalisee = langue.trim().toLowerCase(java.util.Locale.ROOT);
        Predicate langueOriginale = criteriaBuilder.equal(
                criteriaBuilder.lower(conte.get("langueOriginale")), langueNormalisee);

        Subquery<Integer> traductionDansLaLangue = query.subquery(Integer.class);
        Root<ConteDanseTraduction> traduction = traductionDansLaLangue.from(ConteDanseTraduction.class);
        traductionDansLaLangue.select(criteriaBuilder.literal(1))
                .where(
                        criteriaBuilder.equal(traduction.get("conte"), conte),
                        criteriaBuilder.equal(criteriaBuilder.lower(traduction.get("langue")), langueNormalisee),
                        criteriaBuilder.equal(traduction.get("statut"), StatutTraduction.publiee));

        predicates.add(criteriaBuilder.or(langueOriginale, criteriaBuilder.exists(traductionDansLaLangue)));
    }

    private void ajouterEgaliteIgnoree(List<Predicate> predicates,
                                       CriteriaBuilder criteriaBuilder,
                                       Expression<String> expression,
                                       String value) {
        if (estRenseigne(value)) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(expression), value.trim().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private boolean estRenseigne(String value) {
        return value != null && !value.isBlank();
    }

    // ────────────────────────────────────────────────────────────────
    // DETAIL — public
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ConteDanseResponse getById(UUID id) {
        ConteDanse conte = findById(id);
        List<ConteDanseTraduction> traductions = traductionRepository.findByConteIdAndStatut(
                conte.getId(), StatutTraduction.publiee);
        return toResponse(conte, traductions.stream().map(this::toTraductionResponse).toList());
    }

    ConteDanse findById(UUID id) {
        return conteDanseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_CONTE_DANSE,
                        "Conte danse non trouve avec l'ID: " + id));
    }

    ConteDanse verrouillerPourHistorique(UUID id) {
        ConteDanse conte = findById(id);
        entityManager.flush();
        entityManager.refresh(conte, LockModeType.PESSIMISTIC_WRITE);
        return conte;
    }

    // ────────────────────────────────────────────────────────────────
    // RECHERCHE — utilisee par RechercheController
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ConteDanseResponse> rechercher(String query, Pageable pageable) {
        return ajouterTraductions(conteDanseRepository
                .rechercherPublies(StatutConte.publie, query, pageable));
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    /**
     * Verifie que l'utilisateur est l'auteur du conte ou un admin.
     * Leve AccessDeniedException sinon.
     */
    private void verifierAuteurOuAdmin(ConteDanse conte, Utilisateur utilisateur, Jwt jwt) {
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        boolean estAuteur = conte.getCreateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException(
                    "Vous ne pouvez modifier/supprimer que vos propres contes danses");
        }
    }

    private ConteDanseResponse toResponse(ConteDanse conte) {
        return toResponse(conte, List.of());
    }

    private ConteDanseResponse toResponse(ConteDanse conte,
                                          List<ConteDanseTraductionResponse> traductions) {
        return new ConteDanseResponse(
                conte.getId(),
                conte.getTitre(),
                conte.getDescription(),
                conte.getThematique(),
                conte.getLangueOriginale(),
                conte.getCouvertureUrl(),
                conte.getPays(),
                conte.getCulture(),
                conte.getAgeMin(),
                conte.getAgeMax(),
                conte.getDureeMinutes(),
                conte.getCredits(),
                conte.getStatut(),
                conte.getAcces(),
                conte.getIsbn(),
                conte.getFichierTexteUrl(),
                conte.getFichierAudioUrl(),
                conte.getFichierVideoUrl(),
                conte.getMotifModeration(),
                conte.getDateCreation(),
                conte.getDatePublication(),
                conte.getCreateur().getId(),
                conte.getCreateur().getNom(),
                conte.getCreateur().getPrenom(),
                conte.getEcole() != null ? conte.getEcole().getId() : null,
                conte.getClasse() != null ? conte.getClasse().getId() : null,
                traductions);
    }

    private ConteDanseTraductionResponse toTraductionResponse(ConteDanseTraduction traduction) {
        return new ConteDanseTraductionResponse(
                traduction.getId(),
                traduction.getConte().getId(),
                traduction.getLangue(),
                traduction.getVariante(),
                traduction.getTexte(),
                traduction.getAudioUrl(),
                traduction.getVideoUrl(),
                traduction.getSousTitresUrl(),
                traduction.getTraducteur() != null ? traduction.getTraducteur().getId() : null,
                traduction.getTraducteur() != null ? traduction.getTraducteur().getNom() : null,
                traduction.getRelecteur() != null ? traduction.getRelecteur().getId() : null,
                traduction.getRelecteur() != null ? traduction.getRelecteur().getNom() : null,
                traduction.getStatut(),
                traduction.getDateCreation(),
                traduction.getDateMiseAJour());
    }
}
