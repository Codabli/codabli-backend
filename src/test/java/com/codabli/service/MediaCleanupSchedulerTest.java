package com.codabli.service;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class MediaCleanupSchedulerTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaLifecycleService lifecycleService;

    @Mock
    private S3MediaStorageService storageService;

    private MediaCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MediaCleanupScheduler(
                mediaRepository, lifecycleService, storageService, Duration.ofDays(1));
    }

    @Test
    void deletionFailureReturnsDurableItemToRetryQueue() {
        UUID mediaId = UUID.randomUUID();
        Media candidate = media(mediaId, MediaStatus.PENDING_DELETE);
        when(mediaRepository.findTop100ByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(mediaRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(candidate));
        when(lifecycleService.claimDeletion(mediaId)).thenReturn(Optional.of(candidate.getStorageKey()));
        doThrow(new IllegalStateException("S3 unavailable")).when(storageService).delete(candidate.getStorageKey());

        scheduler.cleanUp();

        verify(lifecycleService).retryDeletion(mediaId);
        verify(lifecycleService, never()).completeDeletion(mediaId);
    }

    @Test
    void expiredUploadOrFailureIsQueuedForOrphanCleanup() {
        UUID mediaId = UUID.randomUUID();
        Media candidate = media(mediaId, MediaStatus.FAILED);
        when(mediaRepository.findTop100ByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(candidate));
        when(mediaRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of());

        scheduler.cleanUp();

        verify(lifecycleService).queueOrphanDeletion(
                org.mockito.ArgumentMatchers.eq(mediaId), org.mockito.ArgumentMatchers.any());
    }

    private Media media(UUID id, MediaStatus status) {
        return Media.builder()
                .id(id)
                .storageKey("media/" + id + ".pdf")
                .originalName("document.pdf")
                .contentType("application/pdf")
                .sizeBytes(1024)
                .status(status)
                .createdAt(OffsetDateTime.now().minusDays(2))
                .build();
    }
}
