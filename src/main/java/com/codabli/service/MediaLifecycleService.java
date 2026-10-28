package com.codabli.service;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.exception.ResourceNotFoundException;
import com.codabli.repository.MediaRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MediaLifecycleService {

    private final MediaRepository mediaRepository;
    private final RessourcePedagogiqueRepository ressourceRepository;

    public MediaLifecycleService(
            MediaRepository mediaRepository,
            RessourcePedagogiqueRepository ressourceRepository) {
        this.mediaRepository = mediaRepository;
        this.ressourceRepository = ressourceRepository;
    }

    @Transactional
    public void requestDeletion(UUID mediaId) {
        Media media = lockMedia(mediaId);
        if (isDeletionQueued(media.getStatus())) {
            return;
        }
        if (ressourceRepository.existsByFichierMediaId(mediaId)) {
            throw new BusinessException(ErrorCode.MEDIA_IN_USE,
                    "Le média est toujours associé à une ressource pédagogique.");
        }
        media.setStatus(MediaStatus.PENDING_DELETE);
        mediaRepository.saveAndFlush(media);
    }

    @Transactional
    public void queueDeletionIfUnreferenced(UUID mediaId) {
        Media media = lockMedia(mediaId);
        if (isDeletionQueued(media.getStatus())
                || ressourceRepository.existsByFichierMediaId(mediaId)) {
            return;
        }
        media.setStatus(MediaStatus.PENDING_DELETE);
        mediaRepository.saveAndFlush(media);
    }

    @Transactional
    public void queueOrphanDeletion(UUID mediaId, OffsetDateTime cutoff) {
        Optional<Media> candidate = mediaRepository.findByIdForUpdate(mediaId);
        if (candidate.isEmpty()) {
            return;
        }
        Media media = candidate.get();
        if ((media.getStatus() != MediaStatus.PENDING_UPLOAD && media.getStatus() != MediaStatus.FAILED)
                || media.getCreatedAt().isAfter(cutoff)
                || ressourceRepository.existsByFichierMediaId(mediaId)) {
            return;
        }
        media.setStatus(MediaStatus.PENDING_DELETE);
        mediaRepository.saveAndFlush(media);
    }

    @Transactional
    public Optional<String> claimDeletion(UUID mediaId) {
        Optional<Media> candidate = mediaRepository.findByIdForUpdate(mediaId);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        Media media = candidate.get();
        if (!isDeletionQueued(media.getStatus())) {
            return Optional.empty();
        }
        if (ressourceRepository.existsByFichierMediaId(mediaId)) {
            media.setStatus(MediaStatus.AVAILABLE);
            mediaRepository.saveAndFlush(media);
            return Optional.empty();
        }
        media.setStatus(MediaStatus.DELETING);
        mediaRepository.saveAndFlush(media);
        return Optional.of(media.getStorageKey());
    }

    @Transactional
    public void completeDeletion(UUID mediaId) {
        Optional<Media> candidate = mediaRepository.findByIdForUpdate(mediaId);
        if (candidate.isEmpty()) {
            return;
        }
        Media media = candidate.get();
        if (media.getStatus() != MediaStatus.DELETING) {
            return;
        }
        if (ressourceRepository.existsByFichierMediaId(mediaId)) {
            media.setStatus(MediaStatus.AVAILABLE);
            mediaRepository.saveAndFlush(media);
            return;
        }
        mediaRepository.delete(media);
        mediaRepository.flush();
    }

    @Transactional
    public void retryDeletion(UUID mediaId) {
        Optional<Media> candidate = mediaRepository.findByIdForUpdate(mediaId);
        if (candidate.isEmpty()) {
            return;
        }
        Media media = candidate.get();
        if (media.getStatus() == MediaStatus.DELETING) {
            media.setStatus(MediaStatus.PENDING_DELETE);
            mediaRepository.saveAndFlush(media);
        }
    }

    private Media lockMedia(UUID mediaId) {
        return mediaRepository.findByIdForUpdate(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.UNKNOWN_MEDIA, "Média non trouvé avec l'ID: " + mediaId));
    }

    private boolean isDeletionQueued(MediaStatus status) {
        return status == MediaStatus.PENDING_DELETE || status == MediaStatus.DELETING;
    }
}
