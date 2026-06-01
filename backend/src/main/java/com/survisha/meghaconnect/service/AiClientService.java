package com.survisha.meghaconnect.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Backward-compatible chat facade for older AI document intelligence code.
 */
@Service
@RequiredArgsConstructor
public class AiClientService {

    private final LLMProviderService llmProviderService;

    public boolean isAvailable() {
        return llmProviderService.isAvailable();
    }

    public Optional<String> chat(String systemPrompt, String userMessage) {
        return llmProviderService.chat(messages(systemPrompt, userMessage), LlmOptions.builder()
                .module("ai-document-intelligence")
                .promptType("chat")
                .build());
    }

    public Optional<String> chatCompact(String systemPrompt, String userMessage, int maxTok) {
        return llmProviderService.chat(messages(systemPrompt, userMessage), LlmOptions.builder()
                .module("ai-document-intelligence")
                .promptType("chat-compact")
                .maxTokens(maxTok)
                .build());
    }

    private List<LlmMessage> messages(String systemPrompt, String userMessage) {
        return List.of(
                LlmMessage.builder().role("system").content(systemPrompt).build(),
                LlmMessage.builder().role("user").content(userMessage).build()
        );
    }
}
