package com.survisha.meghaconnect.formextraction.provider;

import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField;
import com.survisha.meghaconnect.formextraction.dto.ImageQualityResult;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class VisitorFormExtractionResult {
    String documentType;
    String formVersion;
    ExtractedVisitorField<String> extractedName;
    ExtractedVisitorField<String> extractedMobileNumber;
    ExtractedVisitorField<Integer> extractedAge;
    ExtractedVisitorField<String> extractedAddress;
    List<String> warnings;
    boolean requiresManualReview;
    ImageQualityResult imageQuality;
    AIProviderType provider;
    String model;
    String providerRequestId;
    long processingTimeMs;
    LocalDateTime extractionTimestamp;
}
