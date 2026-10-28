package com.codabli.service;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MediaRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaLifecycleServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private RessourcePedagogiqueRepository resourceRepository;

    @InjectMocks
    private MediaLifecycleService service;

    private final UUID mediaId = UUID.randomUUID();

    @Test
    void requestDeletionPersistsRetryableStateInsteadOfCallingS3() {
        Media media = media(MediaStatus.AVAILABLE);
        when(mediaRepository.findByIdForUpdate(mediaId)).thenReturn(Optional.of(media));
        when(resourceRepository.existsByFichierMediaId(mediaId)).thenReturn(false);

        service.requestDeletion(mediaId);

        assertEquals(MediaStatus.PENDING_DELETE, media.getStatus());
        verify(mediaRepository).saveAndFlush(media);
    }

    @Test
    void requestDeletionRejectsMediaStillReferencedByBusinessResource() {
        when(mediaRepository.findByIdForUpdate(mediaId))
                .thenReturn(Optional.of(media(MediaStatus.AVAILABLE)));
        when(resourceRepository.existsByFichierMediaId(mediaId)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requestDeletion(mediaId));

        assertEquals(ErrorCode.MEDIA_IN_USE, exception.getErrorCode());
        verify(mediaRepository, never()).saveAndFlush(any(Media.class));
    }

    @Test
    void deletionClaimPersistsInProgressStateForCrashRecovery() {
        Media media = media(MediaStatus.PENDING_DELETE);
        when(mediaRepository.findByIdForUpdate(mediaId)).thenReturn(Optional.of(media));
        when(resourceRepository.existsByFichierMediaId(mediaId)).thenReturn(false);

        Optional<String> key = service.claimDeletion(mediaId);

        assertEquals(Optional.of("media/" + mediaId + ".pdf"), key);
        assertEquals(MediaStatus.DELETING, media.getStatus());
        verify(mediaRepository).saveAndFlush(media);
    }

    @Test
    void deletionClaimRestoresAvailabilityIfReferenceExists() {
        Media media = media(MediaStatus.PENDING_DELETE);
        when(mediaRepository.findByIdForUpdate(mediaId)).thenReturn(Optional.of(media));
        when(resourceRepository.existsByFichierMediaId(mediaId)).thenReturn(true);

        Optional<String> key = service.claimDeletion(mediaId);

        assertEquals(Optional.empty(), key);
        assertEquals(MediaStatus.AVAILABLE, media.getStatus());
    }

    @Test
    void failedDeleteCanBeRetriedAndSuccessfulDeleteRemovesMetadata() {
        Media media = media(MediaStatus.DELETING);
        when(mediaRepository.findByIdForUpdate(mediaId)).thenReturn(Optional.of(media));
        when(resourceRepository.existsByFichierMediaId(mediaId)).thenReturn(false);

        service.retryDeletion(mediaId);
        assertEquals(MediaStatus.PENDING_DELETE, media.getStatus());

        media.setStatus(MediaStatus.DELETING);
        service.completeDeletion(mediaId);
        verify(mediaRepository).delete(media);
        verify(mediaRepository).flush();
    }

    private Media media(MediaStatus status) {
        return Media.builder()
                .id(mediaId)
                .storageKey("media/" + mediaId + ".pdf")
                .originalName("document.pdf")
                .contentType("application/pdf")
                .sizeBytes(1024)
                .status(status)
                .createdAt(OffsetDateTime.now().minusDays(2))
                .build();
    }
}
