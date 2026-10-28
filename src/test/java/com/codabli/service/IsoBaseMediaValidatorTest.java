package com.codabli.service;

import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsoBaseMediaValidatorTest {

    @TempDir
    Path directory;

    private final MediaContentValidator validator = new MediaContentValidator();

    @ParameterizedTest
    @ValueSource(strings = {"avif", "avis", "heic", "heix", "mif1", "msf1", "zzzz", "    "})
    void rejectsUnsupportedBrandsRenamedAsMp4(String brand) throws Exception {
        Path file = writeHeader(brand);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(file, "video.mp4"));

        assertEquals(ErrorCode.INVALID_MEDIA, exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"isom", "iso2", "iso3", "iso4", "iso5", "iso6", "iso7", "iso8", "iso9",
            "mp41", "mp42", "avc1", "dash"})
    void acceptsExplicitlySupportedMp4Brands(String brand) throws Exception {
        var result = validator.validate(writeHeader(brand), "video.mp4");

        assertEquals("mp4", result.extension());
        assertEquals("video/mp4", result.contentType());
    }

    @ParameterizedTest
    @CsvSource(value = {"M4A |m4a|audio/mp4", "M4V |m4v|video/mp4", "qt  |mov|video/quicktime",
            "3gp4|3gp|video/3gpp"}, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void preservesSupportedContainerTypesAndRejectsRenamingAsMp4(
            String brand, String extension, String contentType) throws Exception {
        Path file = writeHeader(brand);
        var result = validator.validate(file, "media." + extension);

        assertEquals(extension, result.extension());
        assertEquals(contentType, result.contentType());
        assertThrows(BusinessException.class, () -> validator.validate(file, "video.mp4"));
    }

    private Path writeHeader(String brand) throws Exception {
        byte[] header = ByteBuffer.allocate(16)
                .putInt(16)
                .put("ftyp".getBytes(StandardCharsets.US_ASCII))
                .put(brand.getBytes(StandardCharsets.US_ASCII))
                .putInt(0)
                .array();
        Path file = directory.resolve("media.bin");
        Files.write(file, header);
        return file;
    }
}
