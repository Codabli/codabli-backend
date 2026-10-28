package com.codabli.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class MediaKeyGenerator {

    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,10}");

    public String generate(UUID mediaId, String detectedExtension) {
        String extension = detectedExtension.toLowerCase(Locale.ROOT);
        if (!SAFE_EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("Extension de média invalide.");
        }
        return mediaId + "." + extension;
    }
}
