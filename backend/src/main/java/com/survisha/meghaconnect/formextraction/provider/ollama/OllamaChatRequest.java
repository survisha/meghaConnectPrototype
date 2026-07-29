package com.survisha.meghaconnect.formextraction.provider.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
public class OllamaChatRequest {
    private String model;
    private boolean stream;
    private List<OllamaMessage> messages;
    private Map<String,Object> format;
    private OllamaOptions options;
    @JsonProperty("keep_alive") private String keepAlive;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OllamaMessage {
        private String role;
        private String content;
        private List<String> images;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OllamaOptions { private double temperature; }
}
