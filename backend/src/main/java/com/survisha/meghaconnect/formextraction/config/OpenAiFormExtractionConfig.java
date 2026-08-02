package com.survisha.meghaconnect.formextraction.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(FormExtractionProperties.class)
public class OpenAiFormExtractionConfig {
    private final FormExtractionProperties properties;
    private final Environment environment;

    public OpenAiFormExtractionConfig(FormExtractionProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateProductionConfiguration() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod") && properties.isEnabled()
                && properties.getProvider() == com.survisha.meghaconnect.formextraction.provider.AIProviderType.OPENAI
                && (blank(properties.getOpenai().getApiKey()) || blank(properties.getOpenai().getModel()))) {
            throw new IllegalStateException("OpenAI form extraction requires OPENAI_API_KEY and OPENAI_FORM_EXTRACTION_MODEL in production");
        }
    }

    @Bean
    public OkHttpClient openAiFormExtractionOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(properties.getOpenai().getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(properties.getOpenai().getTimeoutSeconds(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    @Bean
    public OkHttpClient ollamaFormExtractionOkHttpClient() {
        FormExtractionProperties.Ollama ollama = properties.getOllama();
        return new OkHttpClient.Builder()
                .connectTimeout(ollama.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(ollama.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(ollama.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(ollama.getCallTimeoutSeconds(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
