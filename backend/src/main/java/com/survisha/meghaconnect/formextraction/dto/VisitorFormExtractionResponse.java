package com.survisha.meghaconnect.formextraction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitorFormExtractionResponse {
    boolean success;
    String documentType;
    String formVersion;
    ExtractedVisitorField<String> name;
    ExtractedVisitorField<String> mobileNumber;
    ExtractedVisitorField<Integer> age;
    ExtractedVisitorField<String> address;
    List<String> warnings;
    boolean requiresManualReview;
    ImageQualityResult imageQuality;
    String requestId;
    LocalDateTime extractionTimestamp;
    String modelVersion;
    String message;
}
