package com.survisha.meghaconnect.service;

import java.util.Optional;

public interface LlmProvider {
    String providerName();
    String modelName();
    Optional<String> generateText(String prompt, LlmOptions options);
    LlmHealth healthCheck();
}
