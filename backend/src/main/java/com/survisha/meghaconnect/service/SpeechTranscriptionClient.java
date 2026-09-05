package com.survisha.meghaconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@Service
public class SpeechTranscriptionClient {
    private final RestTemplate rest;
    @Value("${speech.service-url:http://speech-service:8000}") private String serviceUrl;
    @Value("${speech.enabled:false}") private boolean enabled;

    public SpeechTranscriptionClient(RestTemplateBuilder builder,
                                     @Value("${speech.connect-timeout-seconds:3}") long connect,
                                     @Value("${speech.read-timeout-seconds:90}") long read) {
        this.rest = builder.setConnectTimeout(Duration.ofSeconds(connect)).setReadTimeout(Duration.ofSeconds(read)).build();
    }

    @SuppressWarnings("unchecked")
    public Result transcribe(Path audio) {
        if (!enabled) throw new IllegalStateException("Local speech transcription is disabled.");
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", new FileSystemResource(audio));
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<Map> response = rest.postForEntity(serviceUrl + "/transcribe", new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> value = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || value == null) throw new IllegalStateException("Speech service returned no result.");
        return new Result(text(value.get("text")), text(value.get("language")), bool(value.get("needs_review")), text(value.get("warning")));
    }
    private String text(Object value) { return value == null ? null : value.toString().trim(); }
    private boolean bool(Object value) { return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)); }
    @lombok.Value public static class Result { String text; String language; boolean needsReview; String warning; }
}
