package com.codabli.service;

import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

@Component
public class MediaContentValidator {

    private static final int SIGNATURE_BYTES = 4096;
    private static final int MAX_FILENAME_CODE_POINTS = 200;

    public ValidatedMediaFile validate(Path file, String untrustedName) {
        String originalName = sanitizeName(untrustedName);
        int extensionSeparator = originalName.lastIndexOf('.');
        if (extensionSeparator <= 0 || extensionSeparator == originalName.length() - 1) {
            throw invalidMedia();
        }

        String extension = originalName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        byte[] signature;
        try (InputStream input = Files.newInputStream(file)) {
            signature = input.readNBytes(SIGNATURE_BYTES);
        } catch (IOException _) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Impossible de lire le fichier temporaire.");
        }

        DetectedType detectedType = detect(signature, extension);
        if (detectedType == null) {
            throw invalidMedia();
        }
        return new ValidatedMediaFile(originalName, detectedType.extension(), detectedType.contentType());
    }

    private String sanitizeName(String untrustedName) {
        if (untrustedName == null || untrustedName.isBlank()) {
            throw invalidMedia();
        }

        String basename = untrustedName.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        basename = Normalizer.normalize(basename, Normalizer.Form.NFC);

        StringBuilder cleaned = new StringBuilder();
        basename.codePoints()
                .filter(codePoint -> {
                    int type = Character.getType(codePoint);
                    return type != Character.CONTROL && type != Character.FORMAT;
                })
                .forEach(cleaned::appendCodePoint);

        String result = cleaned.toString().trim();
        if (result.isBlank()) {
            throw invalidMedia();
        }
        return truncateFilename(result);
    }

    private String truncateFilename(String name) {
        int codePointCount = name.codePointCount(0, name.length());
        if (codePointCount <= MAX_FILENAME_CODE_POINTS) {
            return name;
        }

        int extensionSeparator = name.lastIndexOf('.');
        String extension = extensionSeparator > 0 ? name.substring(extensionSeparator) : "";
        int extensionLength = extension.codePointCount(0, extension.length());
        int prefixLength = Math.max(1, MAX_FILENAME_CODE_POINTS - extensionLength);
        int end = name.offsetByCodePoints(0, prefixLength);
        String prefix = name.substring(0, end);
        return extension.isEmpty() ? prefix : prefix + extension;
    }

    private DetectedType detect(byte[] bytes, String extension) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return matchesExtension(extension, "jpg", "jpeg")
                    ? new DetectedType("jpg", "image/jpeg") : null;
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return exact(extension, "png", "image/png");
        }
        if (startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a")) {
            return exact(extension, "gif", "image/gif");
        }
        if (startsWithAscii(bytes, "RIFF") && containsAsciiAt(bytes, 8, "WEBP")) {
            return exact(extension, "webp", "image/webp");
        }
        if (startsWithAscii(bytes, "BM")) {
            return exact(extension, "bmp", "image/bmp");
        }
        if (startsWith(bytes, 0x49, 0x49, 0x2A, 0x00)
                || startsWith(bytes, 0x4D, 0x4D, 0x00, 0x2A)) {
            return matchesExtension(extension, "tif", "tiff")
                    ? new DetectedType("tiff", "image/tiff") : null;
        }
        if (startsWithAscii(bytes, "%PDF-")) {
            return exact(extension, "pdf", "application/pdf");
        }
        if (startsWithAscii(bytes, "ID3") || isMpegAudioFrame(bytes)) {
            return exact(extension, "mp3", "audio/mpeg");
        }
        if (startsWithAscii(bytes, "RIFF") && containsAsciiAt(bytes, 8, "WAVE")) {
            return exact(extension, "wav", "audio/wav");
        }
        if (startsWithAscii(bytes, "OggS")) {
            if (matchesExtension(extension, "ogg", "oga", "opus")) {
                String contentType = "audio/ogg";
                if (extension.equals("opus")) {
                    contentType = "audio/opus";
                }
                return new DetectedType(extension, contentType);
            }
            return null;
        }
        if (startsWithAscii(bytes, "fLaC")) {
            return exact(extension, "flac", "audio/flac");
        }
        if (isAacFrame(bytes)) {
            return exact(extension, "aac", "audio/aac");
        }
        if (containsAsciiAt(bytes, 4, "ftyp")) {
            return detectIsoBaseMedia(bytes, extension);
        }
        if (startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return detectEbml(bytes, extension);
        }
        if (startsWithAscii(bytes, "RIFF") && containsAsciiAt(bytes, 8, "AVI ")) {
            return exact(extension, "avi", "video/x-msvideo");
        }
        if (startsWith(bytes, 0x00, 0x00, 0x01, 0xBA)
                || startsWith(bytes, 0x00, 0x00, 0x01, 0xB3)) {
            return matchesExtension(extension, "mpg", "mpeg")
                    ? new DetectedType(extension, "video/mpeg") : null;
        }
        return null;
    }

    private DetectedType detectIsoBaseMedia(byte[] bytes, String extension) {
        if (bytes.length < 12) {
            return null;
        }

        String brand = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
        String detectedExtension;
        String contentType;
        if (brand.startsWith("M4A")) {
            detectedExtension = "m4a";
            contentType = "audio/mp4";
        } else if (brand.startsWith("M4V")) {
            detectedExtension = "m4v";
            contentType = "video/mp4";
        } else if (brand.equals("qt  ")) {
            detectedExtension = "mov";
            contentType = "video/quicktime";
        } else if (brand.startsWith("3gp")) {
            detectedExtension = "3gp";
            contentType = "video/3gpp";
        } else if (matchesExtension(brand, "isom", "iso2", "iso3", "iso4", "iso5", "iso6",
                "iso7", "iso8", "iso9", "mp41", "mp42", "avc1", "dash")) {
            detectedExtension = "mp4";
            contentType = "video/mp4";
        } else {
            return null;
        }
        return extension.equals(detectedExtension)
                ? new DetectedType(detectedExtension, contentType) : null;
    }

    private DetectedType detectEbml(byte[] bytes, String extension) {
        String header = new String(bytes, StandardCharsets.ISO_8859_1);
        if (header.contains("webm") && extension.equals("webm")) {
            return new DetectedType("webm", "video/webm");
        }
        if (header.contains("matroska") && extension.equals("mkv")) {
            return new DetectedType("mkv", "video/x-matroska");
        }
        return null;
    }

    private DetectedType exact(String extension, String expectedExtension, String contentType) {
        return extension.equals(expectedExtension)
                ? new DetectedType(expectedExtension, contentType) : null;
    }

    private boolean matchesExtension(String extension, String... expectedExtensions) {
        for (String expectedExtension : expectedExtensions) {
            if (extension.equals(expectedExtension)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }

        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String prefix) {
        return containsAsciiAt(bytes, 0, prefix);
    }

    private boolean containsAsciiAt(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            if (bytes[offset + index] != (byte) value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMpegAudioFrame(byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }

        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 0xFF && (second & 0xE0) == 0xE0 && (second & 0x06) != 0;
    }

    private boolean isAacFrame(byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }

        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 0xFF && (second & 0xF6) == 0xF0;
    }

    private BusinessException invalidMedia() {
        return new BusinessException(ErrorCode.INVALID_MEDIA,
                "Le fichier est vide, son nom est invalide ou son format n'est pas accepté.");
    }

    private record DetectedType(String extension, String contentType) {
    }
}
