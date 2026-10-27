package com.codabli.service;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.codabli.config.OvhStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class S3MediaStorageIntegrationTest {

    private static final String BUCKET_NAME = "codabli-media-storage-integration";

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder()
            .withInitialBuckets(BUCKET_NAME)
            .withSecureConnection(false)
            .withProperty("spring.autoconfigure.exclude",
                    "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration,"
                            + "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration,"
                            + "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
            .silent()
            .build();

    @TempDir
    Path tempDirectory;

    @Mock
    private S3Presigner s3Presigner;

    @Test
    void fileBackedUploadAndDeleteAreCompatibleWithS3Api() throws Exception {
        try (S3Client s3Client = S3_MOCK.createS3ClientV2()) {
            OvhStorageProperties properties = new OvhStorageProperties();
            properties.setBucketName(BUCKET_NAME);
            S3MediaStorageService storage = new S3MediaStorageService(
                    s3Client, s3Presigner, properties);
            byte[] content = "media storage integration".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Path stagedFile = tempDirectory.resolve("media.upload");
            Files.write(stagedFile, content);

            storage.upload("media/test.txt", stagedFile, "text/plain");
            ResponseBytes<GetObjectResponse> stored = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key("media/test.txt")
                    .build());

            assertArrayEquals(content, stored.asByteArray());
            storage.delete("media/test.txt");
            assertThrows(S3Exception.class, () -> s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key("media/test.txt")
                    .build()));
        }
    }
}
