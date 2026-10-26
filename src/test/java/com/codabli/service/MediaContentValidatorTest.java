package com.codabli.service;

import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaContentValidatorTest {

    @TempDir
    Path tempDirectory;

    private final MediaContentValidator validator = new MediaContentValidator();

    @Test
    void detectsCommonImageAndDocumentSignatures() throws Exception {
        assertType("photo.png", new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        }, "png", "image/png");
        assertType("scan.pdf", "%PDF-1.7".getBytes(), "pdf", "application/pdf");
        assertType("photo.webp", "RIFF1234WEBP".getBytes(), "webp", "image/webp");
    }

    @Test
    void detectsCommonAudioAndVideoContainerSignatures() throws Exception {
        assertType("audio.mp3", "ID3".getBytes(), "mp3", "audio/mpeg");
        assertType("audio.wav", "RIFF1234WAVEfmt ".getBytes(), "wav", "audio/wav");
        assertType("video.mp4", new byte[] {
                0, 0, 0, 16, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'
        }, "mp4", "video/mp4");
        assertType("video.webm", new byte[] {
                0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 'w', 'e', 'b', 'm'
        }, "webm", "video/webm");
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchItsExtension() throws Exception {
        Path file = tempDirectory.resolve("script.jpg");
        Files.writeString(file, "<html>not an image</html>");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(file, "script.jpg"));

        assertEquals(ErrorCode.INVALID_MEDIA, exception.getErrorCode());
    }

    @Test
    void rejectsActiveContentEvenWhenItHasAnImageExtension() throws Exception {
        Path file = tempDirectory.resolve("image.svg");
        Files.writeString(file, "<svg xmlns='http://www.w3.org/2000/svg'/>");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(file, "image.svg"));

        assertEquals(ErrorCode.INVALID_MEDIA, exception.getErrorCode());
    }

    @Test
    void stripsUntrustedPathAndControlCharactersFromOriginalName() throws Exception {
        Path file = tempDirectory.resolve("photo.png");
        Files.write(file, new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        });

        ValidatedMediaFile validated = validator.validate(file, "..\\private\\photo\r\n.png");

        assertEquals("photo.png", validated.originalName());
        assertEquals("png", validated.extension());
    }

    private void assertType(
            String name,
            byte[] signature,
            String extension,
            String contentType) throws Exception {
        Path file = tempDirectory.resolve(name);
        Files.write(file, signature);

        ValidatedMediaFile validated = validator.validate(file, name);

        assertEquals(extension, validated.extension());
        assertEquals(contentType, validated.contentType());
    }
}
