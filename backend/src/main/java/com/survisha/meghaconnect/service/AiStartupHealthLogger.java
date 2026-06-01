package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiStartupHealthLogger implements ApplicationRunner {

    private final LLMProviderService llmProviderService;

    @Override
    public void run(ApplicationArguments args) {
        LlmHealth health = llmProviderService.healthCheck();
        log.info("AI provider startup health provider={} model={} available={} message={}",
                health.getProvider(), health.getModel(), health.isAvailable(), health.getMessage());
    }
}
