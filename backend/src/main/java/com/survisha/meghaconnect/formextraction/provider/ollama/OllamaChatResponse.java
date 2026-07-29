package com.survisha.meghaconnect.formextraction.provider.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OllamaChatResponse {
    private String model;
    private OllamaResponseMessage message;
    private Boolean done;
    @JsonProperty("total_duration") private Long totalDuration;

    @Data
    public static class OllamaResponseMessage {
        private String role;
        private String content;
    }
}
