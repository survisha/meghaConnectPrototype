package com.survisha.meghaconnect.face.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(FaceRecognitionProperties.class)
public class FaceRecognitionClientConfig {
    private final FaceRecognitionProperties properties;
    private final Environment environment;

    public FaceRecognitionClientConfig(FaceRecognitionProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateProductionSecrets() {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (production && properties.isEnabled()
                && (properties.getApiKey() == null || properties.getApiKey().isBlank())) {
            throw new IllegalStateException(
                    "FACE_RECOGNITION_API_KEY is required when face recognition is enabled in production");
        }
    }

    @Bean
    public OkHttpClient faceRecognitionOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(properties.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }
}
