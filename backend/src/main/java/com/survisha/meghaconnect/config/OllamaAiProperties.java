package com.survisha.meghaconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaAiProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3.2";
    private String generateEndpoint = "/api/generate";
    private int timeoutSeconds = 120;
    private int maxInputChars = 12000;
}
