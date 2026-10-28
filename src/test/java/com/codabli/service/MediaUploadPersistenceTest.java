package com.codabli.service;

import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MediaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "media.upload.allow-without-antivirus=true")
class MediaUploadPersistenceTest {

    @Autowired
    private MediaUploadService service;

    @Autowired
    private MediaRepository repository;

    @MockitoBean
    private S3MediaStorageService storage;

    @AfterEach
    void removeUploadedMetadata() {
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(key.capture(), any(Path.class), anyString());
        repository.deleteById(UUID.fromString(key.getValue().substring(0, 36)));
    }

    @Test
    void uploadPreservesGeneratedCreationDateAcrossRepositoryTransactions() {
        var response = service.upload(file());

        var persisted = repository.findById(response.id()).orElseThrow();
        assertThat(response.status()).isEqualTo(MediaStatus.AVAILABLE);
        assertThat(persisted.getStatus()).isEqualTo(MediaStatus.AVAILABLE);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getStorageKey()).isEqualTo(response.id() + ".pdf");
        assertThat(persisted.getOriginalName()).isEqualTo("guide.pdf");
        verify(storage, never()).delete(anyString());
    }

    @Test
    void storageFailurePersistsFailedStatusWithoutLosingCreationDate() {
        doThrow(new IllegalStateException("S3 unavailable"))
                .when(storage).upload(anyString(), any(Path.class), anyString());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(file()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(key.capture(), any(Path.class), anyString());
        var persisted = repository.findById(UUID.fromString(key.getValue().substring(0, 36))).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "guide.pdf", "application/pdf",
                "%PDF-1.7\ncontent".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
