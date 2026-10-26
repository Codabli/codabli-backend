package com.codabli.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "media.upload")
public class MediaUploadProperties {

    private boolean allowWithoutAntivirus;

}
