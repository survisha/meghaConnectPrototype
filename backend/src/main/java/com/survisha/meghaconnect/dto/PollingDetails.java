package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested DTO for polling station details in EPIC verification response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollingDetails {
    
    @JsonProperty("pollingpartno")
    private String pollingPartNo;
    
    @JsonProperty("pollingstationpartname")
    private String pollingstationpartname;
}
