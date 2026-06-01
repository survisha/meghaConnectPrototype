package com.survisha.meghaconnect.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final LLMProviderService llmProviderService;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String translateDynamicText(String text, String targetLanguage) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (targetLanguage == null || targetLanguage.isBlank()
                || "english".equalsIgnoreCase(targetLanguage)
                || "en".equalsIgnoreCase(targetLanguage)) {
            return text;
        }

        String cacheKey = targetLanguage.trim().toLowerCase() + "::" + text;
        return cache.computeIfAbsent(cacheKey, key -> translate(text, targetLanguage).orElse(text));
    }

    private Optional<String> translate(String text, String targetLanguage) {
        String prompt = """
                Translate the following MeghaConnect dynamic text to %s.
                Preserve names, IDs, dates, and government department names.
                Return only the translated text.

                Text:
                %s
                """.formatted(targetLanguage, text);
        return llmProviderService.generateText(prompt, LlmOptions.builder()
                .module("translation")
                .promptType("dynamic-text")
                .targetLanguage(targetLanguage)
                .build());
    }
}
