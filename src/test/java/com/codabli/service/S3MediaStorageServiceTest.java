package com.codabli.service;

import com.codabli.config.OvhStorageConfig;
import com.codabli.config.OvhStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3MediaStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @TempDir
    Path tempDirectory;

    @Test
    void uploadUsesFileBackedRequestAndServerSideEncryption() throws Exception {
        S3MediaStorageService storage = new S3MediaStorageService(
                s3Client, s3Presigner, properties());
        Path file = tempDirectory.resolve("upload.tmp");
        Files.writeString(file, "file content");

        storage.upload("media-id.pdf", file, "application/pdf");

        verify(s3Client).putObject(argThat((PutObjectRequest request) ->
                        request.bucket().equals("test-bucket")
                                && request.key().equals("media-id.pdf")
                                && request.contentType().equals("application/pdf")
                                && request.serverSideEncryption() == ServerSideEncryption.AES256),
                argThat((RequestBody body) -> body.optionalContentLength().isPresent()
                        && body.optionalContentLength().get() == 12));
    }

    @Test
    void presignedDownloadUsesSafeAttachmentFilename() {
        OvhStorageProperties properties = properties();
        OvhStorageConfig config = new OvhStorageConfig(properties);
        AwsCredentialsProvider credentialsProvider = config.s3CredentialsProvider();
        try (S3Presigner presigner = config.s3Presigner(credentialsProvider)) {
            S3MediaStorageService storage = new S3MediaStorageService(
                    s3Client, presigner, properties);

            String url = storage.presignDownload(
                    "media-id.jpg", "..\\private\\photo\r\n.jpg");
            String decodedQuery = URLDecoder.decode(
                    java.net.URI.create(url).getRawQuery(), StandardCharsets.UTF_8);

            assertTrue(decodedQuery.contains("response-content-disposition"));
            assertTrue(decodedQuery.contains("photo.jpg"));
            assertFalse(decodedQuery.contains("private"));
            assertFalse(decodedQuery.contains("\r"));
            assertFalse(decodedQuery.contains("\n"));
        }
    }

    private OvhStorageProperties properties() {
        OvhStorageProperties properties = new OvhStorageProperties();
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setRegion("gra");
        properties.setBucketName("test-bucket");
        properties.setEndpoint("http://localhost:9090");
        properties.setPathStyleAccessEnabled(true);
        return properties;
    }
}
