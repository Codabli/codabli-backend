package com.codabli.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ovh.storage")
public class OvhStorageProperties {

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String region;

    @NotBlank
    private String bucketName;

    private String endpoint = "";

    private boolean pathStyleAccessEnabled;

    @NotNull
    private Duration apiCallTimeout = Duration.ofMinutes(15);

    @NotNull
    private Duration apiCallAttemptTimeout = Duration.ofMinutes(5);

    @NotNull
    private Duration presignedUrlTtl = Duration.ofMinutes(15);

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(500);

    @AssertTrue(message = "S3 timeouts and presigned URL TTL must be positive; TTL cannot exceed 15 minutes. Maximum media size must be positive.")
    public boolean isTimeoutConfigurationValid() {
        return apiCallTimeout != null
                && apiCallAttemptTimeout != null
                && presignedUrlTtl != null
                && maxFileSize != null
                && maxFileSize.toBytes() > 0
                && !apiCallTimeout.isNegative()
                && !apiCallTimeout.isZero()
                && !apiCallAttemptTimeout.isNegative()
                && !apiCallAttemptTimeout.isZero()
                && apiCallAttemptTimeout.compareTo(apiCallTimeout) <= 0
                && !presignedUrlTtl.isNegative()
                && !presignedUrlTtl.isZero()
                && presignedUrlTtl.compareTo(Duration.ofMinutes(15)) <= 0;
    }
}
