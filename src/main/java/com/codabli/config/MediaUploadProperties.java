package com.codabli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media.upload")
public record MediaUploadProperties(boolean allowWithoutAntivirus) {
}
