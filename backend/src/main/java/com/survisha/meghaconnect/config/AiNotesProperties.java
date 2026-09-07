package com.survisha.meghaconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter @Component
@ConfigurationProperties(prefix = "ai.notes")
public class AiNotesProperties {
    private String visionModel = "qwen2.5vl:3b";
    private int maxPdfPages = 5;
    private long maxImageBytes = 5_242_880;
    private int renderDpi = 150;
    private int maxLongestSide = 1600;
    private float jpegQuality = 0.82f;
    private int minMeaningfulTextChars = 80;
}
