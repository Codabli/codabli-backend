package com.codabli.service;

import com.codabli.config.MediaUploadProperties;
import com.codabli.config.OvhStorageProperties;
import com.codabli.dto.MediaUploadResponse;
import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    @Mock
    private MediaRepository repository;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private VirusScanner scanner;

    @Mock
    private ObjectProvider<VirusScanner> scannerProvider;

    private S3MediaStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new S3MediaStorageService(s3Client, s3Presigner, properties());
    }

    @Test
    void uploadValidatesScansPersistsAndStoresUsingGeneratedKey() {
        when(repository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MediaUploadService service = service(repository, storage, scanner);
        MockMultipartFile file = pngUpload();

        MediaUploadResponse response = service.upload(file);

        assertEquals("dessin.png", response.originalName());
        assertEquals("image/png", response.contentType());
        assertEquals(MediaStatus.AVAILABLE, response.status());
        org.mockito.ArgumentCaptor<Path> stagedFile = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(scanner).scan(stagedFile.capture());
        assertFalse(Files.exists(stagedFile.getValue()));
        verify(repository, times(2)).saveAndFlush(any(Media.class));
        verify(s3Client).putObject(org.mockito.ArgumentMatchers.argThat(
                        (PutObjectRequest request) -> request.key().equals(response.id() + ".png")),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(s3Client, never()).deleteObject(
                any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }

    @Test
    void antivirusUnavailableFailsClosedBeforePersistenceOrS3() {
        MediaUploadService service = service(repository, storage, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(pngUpload()));

        assertEquals(ErrorCode.MEDIA_SCAN_UNAVAILABLE, exception.getErrorCode());
        verify(repository, never()).saveAndFlush(any(Media.class));
        verify(s3Client, never()).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void uploadsWithoutAntivirusOnlyWhenExplicitFeatureFlagIsEnabled() {
        when(repository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MediaUploadProperties uploadProperties = new MediaUploadProperties();
        uploadProperties.setAllowWithoutAntivirus(true);
        MediaUploadService service = service(repository, storage, null, properties(), uploadProperties);

        MediaUploadResponse response = service.upload(pngUpload());

        assertEquals(MediaStatus.AVAILABLE, response.status());
        verify(repository, times(2)).saveAndFlush(any(Media.class));
        verify(s3Client).putObject(any(PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void configuredAntivirusIsAlwaysUsedEvenWhenBypassFlagIsEnabled() {
        when(repository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MediaUploadProperties uploadProperties = new MediaUploadProperties();
        uploadProperties.setAllowWithoutAntivirus(true);
        MediaUploadService service = service(repository, storage, scanner, properties(), uploadProperties);

        service.upload(pngUpload());

        org.mockito.ArgumentCaptor<Path> stagedFile = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(scanner).scan(stagedFile.capture());
        assertFalse(Files.exists(stagedFile.getValue()));
    }

    @Test
    void antivirusFailureBlocksUploadEvenWhenBypassFlagIsEnabled() {
        doThrow(new IllegalStateException("scan failed")).when(scanner).scan(any(Path.class));
        MediaUploadProperties uploadProperties = new MediaUploadProperties();
        uploadProperties.setAllowWithoutAntivirus(true);
        MediaUploadService service = service(repository, storage, scanner, properties(), uploadProperties);

        assertThrows(IllegalStateException.class, () -> service.upload(pngUpload()));

        verify(repository, never()).saveAndFlush(any(Media.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void storageFailureMarksMetadataFailedAndReturnsServiceUnavailable() {
        List<MediaStatus> persistedStatuses = new ArrayList<>();
        when(repository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            persistedStatuses.add(media.getStatus());
            return media;
        });
        doThrow(S3Exception.builder().message("offline").statusCode(503).build())
                .when(s3Client).putObject(
                        any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                        any(software.amazon.awssdk.core.sync.RequestBody.class));
        MediaUploadService service = service(repository, storage, scanner);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(pngUpload()));

        assertEquals(ErrorCode.MEDIA_STORAGE_UNAVAILABLE, exception.getErrorCode());
        verify(repository, times(2)).saveAndFlush(any(Media.class));
        assertEquals(List.of(MediaStatus.PENDING_UPLOAD, MediaStatus.FAILED), persistedStatuses);
    }

    @Test
    void databaseFailureAfterUploadCompensatesByDeletingS3Object() {
        List<MediaStatus> attemptedStatuses = new ArrayList<>();
        when(repository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            attemptedStatuses.add(media.getStatus());
            if (media.getStatus() == MediaStatus.AVAILABLE) {
                throw new IllegalStateException("database unavailable");
            }
            return media;
        });
        MediaUploadService service = service(repository, storage, scanner);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(pngUpload()));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        verify(s3Client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
        assertEquals(List.of(MediaStatus.PENDING_UPLOAD, MediaStatus.AVAILABLE, MediaStatus.FAILED),
                attemptedStatuses);
    }

    @Test
    void uploadRejectsOversizedFilesBeforeCreatingTemporaryFile() {
        byte[] tooLarge = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", tooLarge);
        OvhStorageProperties limited = properties();
        limited.setMaxFileSize(DataSize.ofBytes(10));
        MediaUploadService service = service(repository, storage, null, limited);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(file));

        assertEquals(ErrorCode.MEDIA_TOO_LARGE, exception.getErrorCode());
        verify(repository, never()).saveAndFlush(any(Media.class));
    }

    private MediaUploadService service(
            MediaRepository repository,
            S3MediaStorageService storage,
            VirusScanner scanner) {
        return service(repository, storage, scanner, properties());
    }

    private MediaUploadService service(
            MediaRepository repository,
            S3MediaStorageService storage,
            VirusScanner scanner,
            OvhStorageProperties properties) {
        return service(repository, storage, scanner, properties, new MediaUploadProperties());
    }

    private MediaUploadService service(
            MediaRepository repository,
            S3MediaStorageService storage,
            VirusScanner scanner,
            OvhStorageProperties properties,
            MediaUploadProperties uploadProperties) {
        if (scanner != null) {
            when(scannerProvider.getIfAvailable()).thenReturn(scanner);
        }
        return new MediaUploadService(repository, storage, new MediaKeyGenerator(),
                new MediaContentValidator(), scannerProvider, properties, uploadProperties);
    }

    private MockMultipartFile pngUpload() {
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        return new MockMultipartFile("file", "dessin.png", "image/png", pngSignature);
    }

    private OvhStorageProperties properties() {
        OvhStorageProperties properties = new OvhStorageProperties();
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setRegion("gra");
        properties.setBucketName("test-bucket");
        properties.setEndpoint("http://localhost:9090");
        properties.setPathStyleAccessEnabled(true);
        return properties;
    }
}
