package com.codabli.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

import java.net.URI;

@Configuration
@EnableConfigurationProperties({OvhStorageProperties.class, MediaUploadProperties.class})
public class OvhStorageConfig {

    private final OvhStorageProperties properties;

    public OvhStorageConfig(OvhStorageProperties properties) {
        this.properties = properties;
    }

    @Bean
    public AwsCredentialsProvider s3CredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(), properties.getSecretKey());
        return StaticCredentialsProvider.create(credentials);
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider s3CredentialsProvider) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(s3CredentialsProvider)
                .serviceConfiguration(s3Configuration())
                .overrideConfiguration(configuration -> configuration
                        .apiCallTimeout(properties.getApiCallTimeout())
                        .apiCallAttemptTimeout(properties.getApiCallAttemptTimeout()));

        builder.endpointOverride(endpoint());
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider s3CredentialsProvider) {
        Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(s3CredentialsProvider)
                .serviceConfiguration(s3Configuration());

        builder.endpointOverride(endpoint());
        return builder.build();
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                .build();
    }

    private URI endpoint() {
        String configuredEndpoint = properties.getEndpoint();
        if (configuredEndpoint == null || configuredEndpoint.isBlank()) {
            return URI.create("https://s3." + properties.getRegion() + ".io.cloud.ovh.net");
        }
        return URI.create(configuredEndpoint);
    }
}
