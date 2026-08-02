package com.survisha.meghaconnect.formextraction.config;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaFormExtractionConfigTest {
    @Test void appliesDedicatedOllamaTimeouts() {
        FormExtractionProperties properties=new FormExtractionProperties();
        OkHttpClient client=new OpenAiFormExtractionConfig(properties,new MockEnvironment())
                .ollamaFormExtractionOkHttpClient();
        assertEquals(10_000,client.connectTimeoutMillis());
        assertEquals(120_000,client.writeTimeoutMillis());
        assertEquals(360_000,client.readTimeoutMillis());
        assertEquals(390_000,client.callTimeoutMillis());
        assertEquals(false,client.retryOnConnectionFailure());
    }
}
