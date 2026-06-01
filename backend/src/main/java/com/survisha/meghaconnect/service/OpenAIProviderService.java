package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.config.AiProperties;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenAIProviderService implements LlmProvider {

    private final AiProperties properties;

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public String modelName() {
        return properties.getOpenai().getModel();
    }

    @Override
    public Optional<String> generateText(String prompt, LlmOptions options) {
        return Optional.empty();
    }

    @Override
    public LlmHealth healthCheck() {
        return LlmHealth.builder()
                .provider(providerName())
                .model(modelName())
                .available(false)
                .message("OpenAI provider placeholder is configured but not enabled for outbound calls.")
                .build();
    }
}
