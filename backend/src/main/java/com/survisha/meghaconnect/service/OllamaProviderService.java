package com.survisha.meghaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.OllamaAiProperties;
import com.survisha.meghaconnect.util.RequestContextUtil;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class OllamaProviderService implements LlmProvider {

    private final OllamaAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OllamaProviderService(OllamaAiProperties properties,
                                 RestTemplateBuilder restTemplateBuilder,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    @Override
    public Optional<String> generateText(String prompt, LlmOptions options) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", properties.getModel());
            payload.put("prompt", limit(prompt));
            payload.put("stream", false);
            if (options != null && options.getMaxTokens() != null && options.getMaxTokens() > 0) {
                Map<String, Object> providerOptions = new HashMap<>();
                providerOptions.put("num_predict", options.getMaxTokens());
                payload.put("options", providerOptions);
            }

            ResponseEntity<String> response = restTemplate.postForEntity(generateUrl(), payload, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String generated = root.path("response").asText("");
            return generated.isBlank() ? Optional.empty() : Optional.of(generated.trim());
        } catch (Exception e) {
            log.warn("Ollama request failed requestId={} url={} error={}",
                    RequestContextUtil.getRequestId(), RequestContextUtil.safeUri(generateUrl()), e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public LlmHealth healthCheck() {
        if (!properties.isEnabled()) {
            return LlmHealth.builder()
                    .provider(providerName())
                    .model(modelName())
                    .available(false)
                    .message("Ollama provider is disabled")
                    .build();
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(tagsUrl(), String.class);
            boolean available = response.getStatusCode().is2xxSuccessful();
            String message = available ? "Ollama is reachable" : "Ollama returned " + response.getStatusCodeValue();
            if (available && response.getBody() != null && !response.getBody().isBlank()) {
                JsonNode models = objectMapper.readTree(response.getBody()).path("models");
                boolean modelFound = false;
                if (models.isArray()) {
                    for (JsonNode modelNode : models) {
                        String name = modelNode.path("name").asText("");
                        if (name.equals(properties.getModel()) || name.startsWith(properties.getModel() + ":")) {
                            modelFound = true;
                            break;
                        }
                    }
                }
                message = modelFound ? "Ollama is reachable and model is installed"
                        : "Ollama is reachable; configured model was not found in /api/tags";
            }
            return LlmHealth.builder()
                    .provider(providerName())
                    .model(modelName())
                    .available(available)
                    .message(message)
                    .build();
        } catch (Exception e) {
            return LlmHealth.builder()
                    .provider(providerName())
                    .model(modelName())
                    .available(false)
                    .message("Ollama health check failed: " + e.getClass().getSimpleName())
                    .build();
        }
    }

    private URI generateUrl() {
        return URI.create(normalizedBaseUrl() + normalizedEndpoint());
    }

    private URI tagsUrl() {
        return URI.create(normalizedBaseUrl() + "/api/tags");
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalizedEndpoint() {
        String endpoint = properties.getGenerateEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return "/api/generate";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    private String limit(String prompt) {
        String value = prompt == null ? "" : prompt.trim();
        int maxChars = Math.max(1000, properties.getMaxInputChars());
        return value.length() > maxChars ? value.substring(0, maxChars) : value;
    }
}
