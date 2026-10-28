package com.codabli.service;

import com.codabli.config.OvhStorageProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Service
public class S3MediaStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OvhStorageProperties properties;

    public S3MediaStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            OvhStorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public void upload(String key, Path stagedFile, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(stagedFile));
    }

    public String presignDownload(String key, String originalName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .responseContentDisposition(ContentDisposition.attachment()
                        .filename(safeDownloadName(originalName), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.getPresignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest signed = s3Presigner.presignGetObject(presignRequest);

        return signed.url().toExternalForm();
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .build());
    }

    private String safeDownloadName(String originalName) {
        String basename = originalName == null ? "" : originalName.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);

        StringBuilder safeName = new StringBuilder();
        for (int index = 0; index < basename.length(); index++) {
            char character = basename.charAt(index);
            if (!Character.isISOControl(character)) {
                safeName.append(character);
            }
        }

        String result = safeName.toString().trim();
        if (result.isEmpty()) {
            return "media";
        }

        return result.length() > 200 ? result.substring(0, 200) : result;
    }
}
