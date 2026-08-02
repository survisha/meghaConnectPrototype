package com.survisha.meghaconnect.formextraction.config;

import com.survisha.meghaconnect.formextraction.provider.AIProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.net.URI;

@Data
@Validated
@ConfigurationProperties(prefix = "ai.form-extraction")
public class FormExtractionProperties {
    private boolean enabled;
    private AIProviderType provider = AIProviderType.OLLAMA;
    @Min(1) private long maxImageSizeBytes = 5_242_880;
    @Min(1) private int connectTimeoutSeconds = 10;
    @Min(1) private int readTimeoutSeconds = 120;
    private String formVersion = "V1";
    private String languageHint = "en";
    @Min(1) private int minImageWidth = 640;
    @Min(1) private int minImageHeight = 480;
    @Min(1) private int maxNameLength = 150;
    @Min(1) private int maxAddressLength = 500;
    @Min(1) private int minAge = 1;
    @Min(1) private int maxAge = 120;
    @Valid private Ollama ollama = new Ollama();
    @Valid private Image image = new Image();
    private OpenAi openai = new OpenAi();

    @Data
    public static class Ollama {
        private boolean enabled = true;
        @NotBlank private String baseUrl = "http://127.0.0.1:11434";
        @NotBlank private String model = "qwen2.5vl:3b";
        @NotBlank @Pattern(regexp = "^/[^\\s]*$", message = "must start with '/' and contain no whitespace")
        private String chatPath = "/api/chat";
        private boolean stream;
        private double temperature;
        @Min(1) private int numPredict = 400;
        @Min(1) private int numCtx = 4096;
        @Min(1) private int connectTimeoutSeconds = 10;
        @Min(1) private int writeTimeoutSeconds = 120;
        @Min(1) private int readTimeoutSeconds = 360;
        @Min(1) private int callTimeoutSeconds = 390;
        @NotBlank private String keepAlive = "10m";

        @AssertTrue(message = "call timeout must be greater than or equal to read timeout")
        public boolean isCallTimeoutValid() {
            return callTimeoutSeconds >= readTimeoutSeconds;
        }

        @AssertTrue(message = "base URL must be an absolute HTTP(S) URL")
        public boolean isBaseUrlValid() {
            try {
                URI uri = URI.create(baseUrl);
                return uri.isAbsolute() && uri.getHost() != null
                        && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
            } catch (RuntimeException ex) {
                return false;
            }
        }
    }

    @Data
    public static class OpenAi {
        private boolean enabled;
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model;
        private String responsesPath = "/responses";
        private int timeoutSeconds = 60;
        private boolean storeResponse;
        private String imageDetail = "high";
        private int maxOutputTokens = 1000;
    }

    @Data
    public static class Image {
        @Min(1) private int maxLongestSide = 1280;
        @javax.validation.constraints.DecimalMin("0.1") @javax.validation.constraints.DecimalMax("1.0")
        private double jpegQuality = 0.75;
        @Min(1) private long maxProcessedSizeBytes = 1_048_576;
        private boolean preventSizeIncrease = true;
        private boolean autoRotate = true;
        private boolean cropDocument = true;
        private boolean perspectiveCorrection = true;
    }
}
