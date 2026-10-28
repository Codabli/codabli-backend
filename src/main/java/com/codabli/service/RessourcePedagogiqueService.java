package com.codabli.service;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.dto.MediaDownloadResponse;
import com.codabli.entity.Media;
import com.codabli.entity.RessourceFavorite;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.MediaRepository;
import com.codabli.repository.RessourceFavoriteRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des ressources de la mallette pédagogique.
 */
@Service
@Transactional
public class RessourcePedagogiqueService {

    private final RessourcePedagogiqueRepository ressourcePedagogiqueRepository;
    private final RessourceFavoriteRepository ressourceFavoriteRepository;
    private final MediaRepository mediaRepository;
    private final S3MediaStorageService mediaStorageService;
    private final MediaLifecycleService mediaLifecycleService;

    private final UtilisateurService utilisateurService;

    public RessourcePedagogiqueService(
            RessourcePedagogiqueRepository ressourcePedagogiqueRepository,
            RessourceFavoriteRepository ressourceFavoriteRepository,
            MediaRepository mediaRepository,
            S3MediaStorageService mediaStorageService,
            MediaLifecycleService mediaLifecycleService,
            UtilisateurService utilisateurService) {
        this.ressourcePedagogiqueRepository = ressourcePedagogiqueRepository;
        this.ressourceFavoriteRepository = ressourceFavoriteRepository;
        this.mediaRepository = mediaRepository;
        this.mediaStorageService = mediaStorageService;
        this.mediaLifecycleService = mediaLifecycleService;
        this.utilisateurService = utilisateurService;
    }

    // ────────────────────────────────────────────────────────────────
    // FAVORIS — RES-04
    // ────────────────────────────────────────────────────────────────

    public void ajouterFavori(UUID ressourceId, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        if (ressourceFavoriteRepository.findByUtilisateurIdAndRessourceId(utilisateur.getId(), ressourceId).isPresent()) {
            return;
        }
        RessourcePedagogique ressource = findById(ressourceId);

        RessourceFavorite favori = RessourceFavorite.builder()
                .utilisateur(utilisateur)
                .ressource(ressource)
                .build();
        ressourceFavoriteRepository.save(favori);
    }

    public void retirerFavori(UUID ressourceId, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        ressourceFavoriteRepository.findByUtilisateurIdAndRessourceId(utilisateur.getId(), ressourceId)
                .ifPresent(ressourceFavoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<RessourcePedagogiqueResponse> mesFavoris(Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        return ressourceFavoriteRepository.findByUtilisateurId(utilisateur.getId()).stream()
                .map(f -> toResponse(f.getRessource()))
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RessourcePedagogiqueResponse> lister(
            TypeRessource type,
            String thematique,
            String niveauScolaire,
            Boolean actif,
            Jwt jwt,
            Pageable pageable) {

        Boolean actifFilter = actif;
        if (!hasWritePermission(jwt)) {
            // Enseignant/Professionnel de l'éducation : ne voient que l'actif
            actifFilter = true;
        }

        return ressourcePedagogiqueRepository.findByFilters(type, thematique, niveauScolaire, actifFilter, pageable)
                .map(this::toResponse);
    }

    // ────────────────────────────────────────────────────────────────
    // COCHAGE ID / VISIBILITE
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RessourcePedagogiqueResponse getById(UUID id, Jwt jwt) {
        RessourcePedagogique ressource = findById(id);

        // Vérification explicite du flag actif
        if (!ressource.isActif() && !hasWritePermission(jwt)) {
            throw new AccessDeniedException("Vous n'êtes pas autorise a acceder a cette ressource inactive.");
        }

        return toResponse(ressource);
    }

    public MediaDownloadResponse attacherMedia(UUID ressourceId, UUID mediaId) {
        RessourcePedagogique ressource = findById(ressourceId);
        Media media = findAvailableMedia(mediaId);
        Media previousMedia = ressource.getFichierMedia();
        ressource.setFichierMedia(media);
        ressourcePedagogiqueRepository.saveAndFlush(ressource);
        if (previousMedia != null && !previousMedia.getId().equals(mediaId)) {
            mediaLifecycleService.queueDeletionIfUnreferenced(previousMedia.getId());
        }
        return toMediaDownloadResponse(media);
    }

    public void detacherMedia(UUID ressourceId) {
        RessourcePedagogique ressource = findById(ressourceId);
        Media previousMedia = ressource.getFichierMedia();
        if (previousMedia == null) {
            return;
        }
        ressource.setFichierMedia(null);
        ressourcePedagogiqueRepository.saveAndFlush(ressource);
        mediaLifecycleService.queueDeletionIfUnreferenced(previousMedia.getId());
    }

    @Transactional(readOnly = true)
    public MediaDownloadResponse getMediaUrl(UUID ressourceId, Jwt jwt) {
        RessourcePedagogique ressource = findById(ressourceId);
        if (!ressource.isActif() && !hasWritePermission(jwt)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cette ressource inactive.");
        }
        Media media = ressource.getFichierMedia();
        if (media == null || media.getStatus() != MediaStatus.AVAILABLE) {
            throw new ResourceNotFoundException(ErrorCode.UNKNOWN_MEDIA,
                    "Aucun média disponible n'est associé à cette ressource.");
        }
        return toMediaDownloadResponse(media);
    }

    private Media findAvailableMedia(UUID mediaId) {
        Media media = mediaRepository.findByIdForUpdate(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_MEDIA,
                        "Média non trouvé avec l'ID: " + mediaId));
        if (media.getStatus() != MediaStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.UNKNOWN_MEDIA,
                    "Le média demandé n'est pas disponible.");
        }
        return media;
    }

    private MediaDownloadResponse toMediaDownloadResponse(Media media) {
        return new MediaDownloadResponse(media.getId(), media.getOriginalName(),
                mediaStorageService.presignDownload(media.getStorageKey(), media.getOriginalName()));
    }

    private RessourcePedagogique findById(UUID id) {
        return ressourcePedagogiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNKNOWN_RESSOURCE_PEDAGOGIQUE,
                        "Ressource pedagogique non trouvee avec l'ID: " + id));
    }

    // ────────────────────────────────────────────────────────────────
    // CREER (admin & comite_lecture)
    // ────────────────────────────────────────────────────────────────

    public RessourcePedagogiqueResponse creer(RessourcePedagogiqueRequest request) {
        RessourcePedagogique ressource = RessourcePedagogique.builder()
                .titre(request.titre())
                .description(request.description())
                .type(request.type())
                .fichierUrl(request.fichierUrl())
                .thematique(request.thematique())
                .niveauScolaire(request.niveauScolaire())
                .actif(request.actif() == null || request.actif())
                .build();

        RessourcePedagogique saved = ressourcePedagogiqueRepository.save(ressource);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // MODIFIER (admin & comite_lecture)
    // ────────────────────────────────────────────────────────────────

    public RessourcePedagogiqueResponse modifier(UUID id, RessourcePedagogiqueRequest request) {
        RessourcePedagogique ressource = findById(id);

        ressource.setTitre(request.titre());
        ressource.setDescription(request.description());
        ressource.setType(request.type());
        ressource.setFichierUrl(request.fichierUrl());
        ressource.setThematique(request.thematique());
        ressource.setNiveauScolaire(request.niveauScolaire());
        if (request.actif() != null) {
            ressource.setActif(request.actif());
        }

        RessourcePedagogique saved = ressourcePedagogiqueRepository.save(ressource);
        return toResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────
    // SUPPRIMER (admin uniquement)
    // ────────────────────────────────────────────────────────────────

    public void supprimer(UUID id, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new AccessDeniedException("Seul un administrateur peut supprimer definitivement une ressource");
        }

        RessourcePedagogique ressource = findById(id);
        Media previousMedia = ressource.getFichierMedia();
        ressourcePedagogiqueRepository.delete(ressource);
        ressourcePedagogiqueRepository.flush();
        if (previousMedia != null) {
            mediaLifecycleService.queueDeletionIfUnreferenced(previousMedia.getId());
        }
    }

    // ────────────────────────────────────────────────────────────────
    // SECURITE INTERNE
    // ────────────────────────────────────────────────────────────────

    private boolean hasWritePermission(Jwt jwt) {
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        return roles.contains("admin") || roles.contains("super_admin") || roles.contains("comite_lecture");
    }

    private boolean isAdmin(Jwt jwt) {
        Collection<String> roles = utilisateurService.extractRoles(jwt);
        return roles.contains("admin") || roles.contains("super_admin");
    }

    // ────────────────────────────────────────────────────────────────
    // MAPPAGE
    // ────────────────────────────────────────────────────────────────

    private RessourcePedagogiqueResponse toResponse(RessourcePedagogique ressource) {
        return new RessourcePedagogiqueResponse(
                ressource.getId(),
                ressource.getTitre(),
                ressource.getDescription(),
                ressource.getType(),
                ressource.getFichierUrl(),
                ressource.getThematique(),
                ressource.getNiveauScolaire(),
                ressource.getDateAjout(),
                ressource.isActif());
    }
}
