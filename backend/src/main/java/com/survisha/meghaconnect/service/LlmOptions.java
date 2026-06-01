package com.survisha.meghaconnect.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmOptions {
    private String module;
    private String promptType;
    private Integer maxTokens;
    private String targetLanguage;
}
