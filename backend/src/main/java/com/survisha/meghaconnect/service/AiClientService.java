package com.survisha.meghaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.OllamaAiProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Local AI client backed by Ollama.
 *
 * <p>This preserves the previous chat-style public methods used by document
 * intelligence while keeping all model calls on the server-side local Ollama API.</p>
 */
@Service
@RequiredArgsConstructor
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    private final OllamaAiProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    public boolean isAvailable() {
        return properties.isEnabled();
    }

    public Optional<String> chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        return generate(buildPrompt(systemPrompt, userMessage));
    }

    public Optional<String> chatCompact(String systemPrompt, String userMessage, int maxTok) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        return generate(buildPrompt(systemPrompt, userMessage));
    }

    private Optional<String> generate(String prompt) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("prompt", limit(prompt));
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();

            String responseBody = restTemplate.postForObject(
                    ollamaGenerateUrl(),
                    new HttpEntity<>(body, headers),
                    String.class);

            return Optional.of(readResponseText(responseBody));
        } catch (RestClientException e) {
            log.warn("Ollama AI call failed: {}", safeError(e));
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Ollama AI response handling failed: {}", safeError(e));
            return Optional.empty();
        }
    }

    private String readResponseText(String responseBody) throws java.io.IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode response = root.get("response");
        if (response == null || response.asText().isBlank()) {
            throw new IllegalStateException("Ollama response did not include a response field.");
        }
        return response.asText().trim();
    }

    private String buildPrompt(String systemPrompt, String userMessage) {
        String system = systemPrompt == null ? "" : systemPrompt.trim();
        String user = userMessage == null ? "" : userMessage.trim();
        return """
                System instructions:
                %s

                User content:
                %s
                """.formatted(system, user);
    }

    private String limit(String prompt) {
        String normalized = prompt == null ? "" : prompt.trim();
        int maxChars = Math.max(1000, properties.getMaxInputChars());
        return normalized.length() > maxChars ? normalized.substring(0, maxChars) : normalized;
    }

    private String ollamaGenerateUrl() {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String endpoint = properties.getGenerateEndpoint() != null
                ? properties.getGenerateEndpoint().trim()
                : "/api/generate";
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    private String safeError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
