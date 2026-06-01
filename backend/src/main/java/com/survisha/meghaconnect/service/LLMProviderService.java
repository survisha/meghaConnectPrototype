package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.config.AiProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMProviderService {

    private static final LlmOptions DEFAULT_OPTIONS = LlmOptions.builder()
            .module("ai")
            .promptType("generateText")
            .build();

    private final AiProperties properties;
    private final List<LlmProvider> providers;
    private final AiAuditService auditService;

    public Optional<String> generateText(String prompt, LlmOptions options) {
        return invoke(prompt, normalize(options, "generateText"));
    }

    public Optional<String> generateJson(String prompt, String schema, LlmOptions options) {
        String jsonPrompt = prompt + "\n\nReturn valid JSON only.";
        if (schema != null && !schema.isBlank()) {
            jsonPrompt += "\nJSON schema/shape:\n" + schema;
        }
        return invoke(jsonPrompt, normalize(options, "generateJson"));
    }

    public Optional<String> summarizeDocument(String text, LlmOptions options) {
        String prompt = """
                Summarize the following MeghaConnect appointment/supporting document for government staff.
                Keep the response concise, factual, and avoid inventing missing details.

                Document:
                %s
                """.formatted(text == null ? "" : text);
        return invoke(prompt, normalize(options, "summarizeDocument"));
    }

    public Optional<String> chat(List<LlmMessage> messages, LlmOptions options) {
        String prompt = messages == null ? "" : messages.stream()
                .map(message -> "%s: %s".formatted(
                        message.getRole() == null ? "user" : message.getRole(),
                        message.getContent() == null ? "" : message.getContent()))
                .collect(Collectors.joining("\n\n"));
        return invoke(prompt, normalize(options, "chat"));
    }

    public LlmHealth healthCheck() {
        return selectedProvider().healthCheck();
    }

    public String providerName() {
        return selectedProvider().providerName();
    }

    public String modelName() {
        return selectedProvider().modelName();
    }

    public boolean isAvailable() {
        return healthCheck().isAvailable();
    }

    private Optional<String> invoke(String prompt, LlmOptions options) {
        LlmProvider provider = selectedProvider();
        LocalDateTime startedAt = LocalDateTime.now();
        long startNanos = System.nanoTime();
        boolean success = false;
        String error = null;
        try {
            Optional<String> result = provider.generateText(prompt, options);
            success = result.isPresent();
            if (result.isEmpty()) {
                error = "Provider returned no response";
            }
            return result;
        } catch (RuntimeException e) {
            error = e.getClass().getSimpleName();
            log.warn("AI provider call failed provider={} module={} promptType={} error={}",
                    provider.providerName(), options.getModule(), options.getPromptType(), e.getClass().getSimpleName());
            return Optional.empty();
        } finally {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            auditService.record(options.getModule(), options.getPromptType(), provider.providerName(),
                    provider.modelName(), startedAt, durationMs, success, error);
        }
    }

    private LlmProvider selectedProvider() {
        String selected = properties.getProvider() == null ? "ollama" : properties.getProvider().trim().toLowerCase(Locale.ROOT);
        return providers.stream()
                .filter(provider -> provider.providerName().equalsIgnoreCase(selected))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Configured AI provider '{}' not found. Falling back to ollama.", selected);
                    return providers.stream()
                            .filter(provider -> provider.providerName().equalsIgnoreCase("ollama"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No AI providers are registered."));
                });
    }

    private LlmOptions normalize(LlmOptions options, String defaultPromptType) {
        LlmOptions source = options == null ? DEFAULT_OPTIONS : options;
        return LlmOptions.builder()
                .module(blankToDefault(source.getModule(), "ai"))
                .promptType(blankToDefault(source.getPromptType(), defaultPromptType))
                .maxTokens(source.getMaxTokens())
                .targetLanguage(source.getTargetLanguage())
                .build();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
