package com.survisha.meghaconnect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "epic-api")
public class EpicApiProperties {
    private boolean enabled;
    @NotBlank
    private String endpoint;
    private String apiKey;
}
