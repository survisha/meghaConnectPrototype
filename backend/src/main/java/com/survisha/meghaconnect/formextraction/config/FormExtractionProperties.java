package com.survisha.meghaconnect.formextraction.config;

import com.survisha.meghaconnect.formextraction.provider.AIProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

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
    private Ollama ollama = new Ollama();
    private OpenAi openai = new OpenAi();

    @Data
    public static class Ollama {
        private boolean enabled = true;
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5vl:7b";
        private String chatPath = "/api/chat";
        private boolean stream;
        private double temperature;
        private int timeoutSeconds = 120;
        private String keepAlive = "5m";
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
}
