package com.survisha.meghaconnect.epic.face.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(EpicFaceProperties.class)
public class EpicFaceClientConfig {
    @Bean
    public OkHttpClient epicFaceOkHttpClient(EpicFaceProperties p) {
        return new OkHttpClient.Builder()
                .connectTimeout(p.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(p.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(p.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(p.getCallTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }
}
