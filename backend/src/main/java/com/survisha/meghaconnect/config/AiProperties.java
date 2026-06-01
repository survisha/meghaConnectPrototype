package com.survisha.meghaconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiProperties {

    private String provider = "ollama";
    private int timeoutSeconds = 60;
    private OpenAi openai = new OpenAi();
    private AzureOpenAi azureOpenai = new AzureOpenAi();

    @Getter
    @Setter
    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o-mini";
    }

    @Getter
    @Setter
    public static class AzureOpenAi {
        private String endpoint;
        private String apiKey;
        private String deployment;
    }
}
