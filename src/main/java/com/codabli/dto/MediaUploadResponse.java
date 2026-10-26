package com.codabli.dto;

import com.codabli.entity.enums.MediaStatus;

import java.util.UUID;

public record MediaUploadResponse(
        UUID id,
        String originalName,
        String contentType,
        long sizeBytes,
        MediaStatus status) {
}
