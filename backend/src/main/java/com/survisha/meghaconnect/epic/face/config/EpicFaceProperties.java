package com.survisha.meghaconnect.epic.face.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Validated
@ConfigurationProperties(prefix = "integration.epic-face")
public class EpicFaceProperties {
    /** Values are supplied by application.yml and the active profile override. */
    private boolean enabled;
    @NotBlank private String baseUrl;
    @NotBlank private String search1nPath;
    @NotBlank private String verify11Path;
    private String searchApiKey;
    private String verifyApiKey;
    @Min(1) private long connectTimeoutSeconds;
    @Min(1) private long readTimeoutSeconds;
    @Min(1) private long writeTimeoutSeconds;
    @Min(1) private long callTimeoutSeconds;
}
