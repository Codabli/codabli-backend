package com.codabli.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvhStorageConfigTest {

    @Test
    void s3PresignerUsesConfiguredEndpointAndPathStyleBucketKey() throws Exception {
        OvhStorageProperties properties = new OvhStorageProperties();
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setRegion("gra");
        properties.setBucketName("test-bucket");
        properties.setEndpoint("http://localhost:9090");
        properties.setPathStyleAccessEnabled(true);

        OvhStorageConfig config = new OvhStorageConfig(properties);
        AwsCredentialsProvider credentialsProvider = config.s3CredentialsProvider();
        try (var presigner = config.s3Presigner(credentialsProvider)) {
            var presignedRequest = presigner.presignGetObject(request -> request
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key("media-id.jpg")
                            .build()));
            URI url = presignedRequest.url().toURI();

            assertEquals("http", url.getScheme());
            assertEquals("localhost", url.getHost());
            assertEquals("/test-bucket/media-id.jpg", url.getPath());
            assertTrue(url.getRawQuery().contains("X-Amz-Signature="));
        }
    }

    @Test
    void s3PresignerUsesOvhEndpointForConfiguredRegionByDefault() throws Exception {
        OvhStorageProperties properties = new OvhStorageProperties();
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setRegion("sbg");
        properties.setBucketName("test-bucket");

        OvhStorageConfig config = new OvhStorageConfig(properties);
        try (var presigner = config.s3Presigner(config.s3CredentialsProvider())) {
            var presignedRequest = presigner.presignGetObject(request -> request
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key("media-id.jpg")
                            .build()));

            assertEquals("test-bucket.s3.sbg.io.cloud.ovh.net", presignedRequest.url().getHost());
        }
    }

    @Test
    void propertiesRejectPresignedUrlsLongerThanFifteenMinutes() {
        OvhStorageProperties properties = new OvhStorageProperties();
        properties.setPresignedUrlTtl(Duration.ofMinutes(16));

        assertFalse(properties.isTimeoutConfigurationValid());
    }
}
