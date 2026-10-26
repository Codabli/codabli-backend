package com.codabli.service;

import com.codabli.dto.MediaDownloadResponse;
import com.codabli.entity.Media;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MediaRepository;
import com.codabli.repository.RessourceFavoriteRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RessourcePedagogiqueMediaServiceTest {

    private final RessourcePedagogiqueRepository resourceRepository =
            mock(RessourcePedagogiqueRepository.class);
    private final RessourceFavoriteRepository favoriteRepository =
            mock(RessourceFavoriteRepository.class);
    private final MediaRepository mediaRepository = mock(MediaRepository.class);
    private final S3MediaStorageService storageService = mock(S3MediaStorageService.class);
    private final MediaLifecycleService mediaLifecycleService = mock(MediaLifecycleService.class);
    private final UtilisateurService utilisateurService = mock(UtilisateurService.class);
    private final RessourcePedagogiqueService service = new RessourcePedagogiqueService(
            resourceRepository, favoriteRepository, mediaRepository, storageService,
            mediaLifecycleService, utilisateurService);

    private final UUID resourceId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final Jwt teacherJwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("teacher")
            .build();

    @BeforeEach
    void configureMedia() {
        when(storageService.presignDownload(mediaId + ".pdf", "guide.pdf"))
                .thenReturn("https://storage.example/signed");
    }

    @Test
    void authorizedUserReceivesSignedUrlForActiveResourceMedia() {
        Media media = media(MediaStatus.AVAILABLE);
        RessourcePedagogique resource = resource(true, media);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        MediaDownloadResponse response = service.getMediaUrl(resourceId, teacherJwt);

        assertEquals(mediaId, response.mediaId());
        assertEquals("guide.pdf", response.originalName());
        assertEquals("https://storage.example/signed", response.url());
        verify(storageService).presignDownload(mediaId + ".pdf", "guide.pdf");
    }

    @Test
    void inactiveResourceIsNotVisibleToTeacherAndDoesNotPresign() {
        RessourcePedagogique resource = resource(false, media(MediaStatus.AVAILABLE));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(utilisateurService.extractRoles(teacherJwt)).thenReturn(List.of("enseignant"));

        assertThrows(AccessDeniedException.class,
                () -> service.getMediaUrl(resourceId, teacherJwt));
    }

    @Test
    void onlyAvailableMediaCanBeAttached() {
        when(mediaRepository.findByIdForUpdate(mediaId))
                .thenReturn(Optional.of(media(MediaStatus.PENDING_UPLOAD)));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource(true, null)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.attacherMedia(resourceId, mediaId));

        assertEquals(ErrorCode.UNKNOWN_MEDIA, exception.getErrorCode());
    }

    @Test
    void attachMediaStoresReferenceAndReturnsSignedAccess() {
        Media media = media(MediaStatus.AVAILABLE);
        RessourcePedagogique resource = resource(true, null);
        when(mediaRepository.findByIdForUpdate(mediaId)).thenReturn(Optional.of(media));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resourceRepository.saveAndFlush(resource)).thenReturn(resource);

        MediaDownloadResponse response = service.attacherMedia(resourceId, mediaId);

        assertEquals(media, resource.getFichierMedia());
        assertEquals("https://storage.example/signed", response.url());
        verify(resourceRepository).saveAndFlush(resource);
    }

    @Test
    void replacingMediaQueuesOldObjectForDeletion() {
        Media previous = media(MediaStatus.AVAILABLE);
        Media replacement = Media.builder()
                .id(UUID.randomUUID())
                .storageKey("replacement.pdf")
                .originalName("replacement.pdf")
                .contentType("application/pdf")
                .sizeBytes(128)
                .status(MediaStatus.AVAILABLE)
                .createdAt(OffsetDateTime.now())
                .build();
        RessourcePedagogique resource = resource(true, previous);
        when(mediaRepository.findByIdForUpdate(replacement.getId())).thenReturn(Optional.of(replacement));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        service.attacherMedia(resourceId, replacement.getId());

        verify(resourceRepository).saveAndFlush(resource);
        verify(mediaLifecycleService).queueDeletionIfUnreferenced(mediaId);
    }

    @Test
    void detachingMediaQueuesOldObjectForDeletionAndKeepsLegacyUrl() {
        Media previous = media(MediaStatus.AVAILABLE);
        RessourcePedagogique resource = resource(true, previous);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        service.detacherMedia(resourceId);

        assertEquals("https://legacy.example/guide.pdf", resource.getFichierUrl());
        assertNull(resource.getFichierMedia());
        verify(resourceRepository).saveAndFlush(resource);
        verify(mediaLifecycleService).queueDeletionIfUnreferenced(mediaId);
    }

    @Test
    void deletingResourceQueuesItsMediaAfterRemovingTheReference() {
        Jwt adminJwt = Jwt.withTokenValue("admin-token")
                .header("alg", "none")
                .subject("admin")
                .build();
        Media previous = media(MediaStatus.AVAILABLE);
        RessourcePedagogique resource = resource(true, previous);
        when(utilisateurService.extractRoles(adminJwt)).thenReturn(List.of("admin"));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        service.supprimer(resourceId, adminJwt);

        var order = inOrder(resourceRepository, mediaLifecycleService);
        order.verify(resourceRepository).delete(resource);
        order.verify(resourceRepository).flush();
        order.verify(mediaLifecycleService).queueDeletionIfUnreferenced(mediaId);
    }

    private Media media(MediaStatus status) {
        return Media.builder()
                .id(mediaId)
                .storageKey(mediaId + ".pdf")
                .originalName("guide.pdf")
                .contentType("application/pdf")
                .sizeBytes(128)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private RessourcePedagogique resource(boolean active, Media media) {
        return RessourcePedagogique.builder()
                .id(resourceId)
                .titre("Guide")
                .fichierUrl("https://legacy.example/guide.pdf")
                .fichierMedia(media)
                .actif(active)
                .dateAjout(OffsetDateTime.now())
                .build();
    }
}
