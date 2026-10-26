package com.codabli.dto;

import java.util.UUID;

public record MediaDownloadResponse(UUID mediaId, String originalName, String url) {
}
