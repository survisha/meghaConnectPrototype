package com.survisha.meghaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.OllamaAiProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OllamaAiNotesService {

    private final OllamaAiProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    public String generateNotes(String documentText) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Ollama AI notes generation is disabled.");
        }
        String prompt = buildPrompt(limit(documentText));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("prompt", prompt);
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
        return readResponseText(responseBody);
    }

    public String getModelName() {
        return properties.getModel();
    }

    private String readResponseText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode response = root.get("response");
            if (response == null || response.asText().isBlank()) {
                throw new IllegalStateException("Ollama response did not include a response field.");
            }
            return response.asText().trim();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read Ollama response.", e);
        }
    }

    private String ollamaGenerateUrl() {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String endpoint = properties.getGenerateEndpoint() != null ? properties.getGenerateEndpoint().trim() : "/api/generate";
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    private String limit(String documentText) {
        String normalized = documentText == null ? "" : documentText.trim();
        int maxChars = Math.max(1000, properties.getMaxInputChars());
        return normalized.length() > maxChars ? normalized.substring(0, maxChars) : normalized;
    }

    private String buildPrompt(String documentText) {
        return """
                You are an AI assistant for MeghaConnect, a citizen appointment and government service management system.

                Read the uploaded citizen document text and generate officer-friendly short notes.

                Return only this format:

                Summary:
                - maximum 5 short bullet points

                Important Details:
                - applicant name if found
                - address if found
                - ID/reference number if found
                - purpose/request if found

                Missing or Unclear Information:
                - mention if any important information is missing
                - if nothing is missing, say Not found

                Risk Flags:
                - mention mismatch, unclear document, missing signature, expired date, or suspicious content if found
                - if no risk found, say Not found

                Rules:
                - Do not assume or invent information.
                - If information is not available, write Not found.
                - Keep the answer short and useful for officers.

                Document Text:
                %s
                """.formatted(documentText);
    }
}
