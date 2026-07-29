package com.survisha.meghaconnect.face.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "integration.face-recognition")
public class FaceRecognitionProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String clientId;
    private String appId;
    @Min(1) private int connectTimeoutSeconds = 10;
    @Min(1) private int readTimeoutSeconds = 60;
    @Min(1) private int writeTimeoutSeconds = 60;
    @Min(1) private long maxPhotoSizeBytes = 5_242_880;
    private List<String> allowedPhotoFormats = List.of("image/jpeg", "image/png");
}
