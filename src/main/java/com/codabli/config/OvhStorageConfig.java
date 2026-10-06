package com.codabli.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class OvhStorageConfig {

    @Value("${ovh.storage.access-key}")
    private String accessKey;

    @Value("${ovh.storage.secret-key}")
    private String secretKey;

    @Value("${ovh.storage.region}")
    private String region;

    /**
     * Pour effectuer les opérations S3 (upload, download, delete, list…) en envoyant des requêtes HTTP au serveur.
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create("https://s3." + region + ".io.cloud.ovh.net"))
                .build();
    }

    /**
     * Génère des URLs signées (pré-signées) sans effectuer l'opération elle-même.
     * Génère un lien temporaire à un client (front/llm) pour qu'il télécharge directement, sans passer par l'API.
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create("https://s3." + region + ".io.cloud.ovh.net"))
                .build();
    }
}
