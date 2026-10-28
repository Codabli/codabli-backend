package com.codabli.service;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "media.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MediaCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(MediaCleanupScheduler.class);

    private final MediaRepository mediaRepository;
    private final MediaLifecycleService lifecycleService;
    private final S3MediaStorageService storageService;
    private final Duration orphanAge;

    public MediaCleanupScheduler(
            MediaRepository mediaRepository,
            MediaLifecycleService lifecycleService,
            S3MediaStorageService storageService,
            @Value("${media.cleanup.orphan-age:24h}") Duration orphanAge) {
        this.mediaRepository = mediaRepository;
        this.lifecycleService = lifecycleService;
        this.storageService = storageService;
        this.orphanAge = orphanAge;
    }

    @Scheduled(fixedDelayString = "${media.cleanup.fixed-delay-ms:3600000}")
    public void cleanUp() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(orphanAge);
        List<Media> orphanCandidates = mediaRepository.findTop100ByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                List.of(MediaStatus.PENDING_UPLOAD, MediaStatus.FAILED), cutoff);
        for (Media media : orphanCandidates) {
            processSafely(media.getId(), () -> lifecycleService.queueOrphanDeletion(media.getId(), cutoff));
        }

        List<Media> deletionCandidates = mediaRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                List.of(MediaStatus.PENDING_DELETE, MediaStatus.DELETING));
        for (Media media : deletionCandidates) {
            processSafely(media.getId(), () -> deleteObject(media.getId()));
        }
    }

    private void deleteObject(UUID mediaId) {
        Optional<String> storageKey = lifecycleService.claimDeletion(mediaId);
        if (storageKey.isEmpty()) {
            return;
        }
        storageService.delete(storageKey.get());
        lifecycleService.completeDeletion(mediaId);
    }

    private void processSafely(UUID mediaId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            try {
                lifecycleService.retryDeletion(mediaId);
            } catch (RuntimeException retryException) {
                exception.addSuppressed(retryException);
            }
            log.error("Media cleanup failed for {}; durable state will be retried", mediaId, exception);
        }
    }
}
