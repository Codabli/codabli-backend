package com.codabli.service;

import com.codabli.config.MediaUploadProperties;
import com.codabli.config.OvhStorageProperties;
import com.codabli.dto.MediaUploadResponse;
import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadService.class);

    private final MediaRepository mediaRepository;
    private final S3MediaStorageService storageService;
    private final MediaKeyGenerator keyGenerator;
    private final MediaContentValidator contentValidator;
    private final ObjectProvider<VirusScanner> virusScannerProvider;
    private final OvhStorageProperties storageProperties;
    private final MediaUploadProperties uploadProperties;

    public MediaUploadService(
            MediaRepository mediaRepository,
            S3MediaStorageService storageService,
            MediaKeyGenerator keyGenerator,
            MediaContentValidator contentValidator,
            ObjectProvider<VirusScanner> virusScannerProvider,
            OvhStorageProperties storageProperties,
            MediaUploadProperties uploadProperties) {
        this.mediaRepository = mediaRepository;
        this.storageService = storageService;
        this.keyGenerator = keyGenerator;
        this.contentValidator = contentValidator;
        this.virusScannerProvider = virusScannerProvider;
        this.storageProperties = storageProperties;
        this.uploadProperties = uploadProperties;
    }

    public MediaUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA, "Le fichier ne peut pas être vide.");
        }
        if (file.getSize() > storageProperties.getMaxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.MEDIA_TOO_LARGE,
                    "Le fichier dépasse la taille maximale autorisée.");
        }

        Path stagedFile = createTemporaryFile();
        try {
            file.transferTo(stagedFile);
            long sizeBytes = Files.size(stagedFile);
            if (sizeBytes == 0) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA, "Le fichier ne peut pas être vide.");
            }
            if (sizeBytes > storageProperties.getMaxFileSize().toBytes()) {
                throw new BusinessException(ErrorCode.MEDIA_TOO_LARGE,
                        "Le fichier dépasse la taille maximale autorisée.");
            }

            ValidatedMediaFile validatedFile = contentValidator.validate(stagedFile, file.getOriginalFilename());
            scanForViruses(stagedFile);

            return storeValidatedFile(stagedFile, validatedFile, sizeBytes);
        } catch (IOException _) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Impossible de préparer le fichier pour son stockage.");
        } finally {
            deleteTemporaryFile(stagedFile);
        }
    }

    private void scanForViruses(Path stagedFile) {
        VirusScanner scanner = virusScannerProvider.getIfAvailable();
        if (scanner != null) {
            scanner.scan(stagedFile);
            return;
        }
        if (!uploadProperties.isAllowWithoutAntivirus()) {
            throw new BusinessException(ErrorCode.MEDIA_SCAN_UNAVAILABLE,
                    "Le service de contrôle antivirus n'est pas configuré.");
        }
        log.warn("Accepting media upload without antivirus scanning because "
                + "media.upload.allow-without-antivirus is enabled.");
    }

    private MediaUploadResponse storeValidatedFile(
            Path stagedFile,
            ValidatedMediaFile validatedFile,
            long sizeBytes) {
        UUID mediaId = UUID.randomUUID();
        String storageKey = keyGenerator.generate(mediaId, validatedFile.extension());
        Media media = Media.builder()
                .id(mediaId)
                .storageKey(storageKey)
                .originalName(validatedFile.originalName())
                .contentType(validatedFile.contentType())
                .sizeBytes(sizeBytes)
                .status(MediaStatus.PENDING_UPLOAD)
                .build();

        try {
            mediaRepository.saveAndFlush(media);
        } catch (RuntimeException exception) {
            log.error("Failed to create media metadata for media {}", mediaId, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer les métadonnées du média.");
        }

        try {
            storageService.upload(storageKey, stagedFile, validatedFile.contentType());
        } catch (RuntimeException exception) {
            markFailed(media);
            log.error("S3 upload failed for media {}", mediaId, exception);
            throw new BusinessException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE,
                    "Le stockage du média est temporairement indisponible.");
        }

        media.setStatus(MediaStatus.AVAILABLE);
        try {
            mediaRepository.saveAndFlush(media);
        } catch (RuntimeException exception) {
            compensateUpload(media);
            log.error("Failed to mark uploaded media {} as available", mediaId, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Le média a été stocké, mais son enregistrement n'a pas pu être finalisé.");
        }
        return new MediaUploadResponse(mediaId, media.getOriginalName(),
                media.getContentType(), media.getSizeBytes(), media.getStatus());
    }

    private void markFailed(Media media) {
        media.setStatus(MediaStatus.FAILED);
        try {
            mediaRepository.saveAndFlush(media);
        } catch (RuntimeException exception) {
            log.error("Failed to mark media {} as failed; it remains pending reconciliation",
                    media.getId(), exception);
        }
    }

    private void compensateUpload(Media media) {
        try {
            storageService.delete(media.getStorageKey());
        } catch (RuntimeException exception) {
            log.error("Failed to compensate S3 upload for media {}; it remains pending reconciliation",
                    media.getId(), exception);
            return;
        }
        markFailed(media);
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile("codabli-media-", ".upload");
        } catch (IOException _) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Impossible de préparer le fichier pour son stockage.");
        }
    }

    private void deleteTemporaryFile(Path stagedFile) {
        try {
            Files.deleteIfExists(stagedFile);
        } catch (IOException exception) {
            log.error("Failed to remove staged media file {}", stagedFile, exception);
        }
    }
}
