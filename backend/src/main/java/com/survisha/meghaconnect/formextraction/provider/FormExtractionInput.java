package com.survisha.meghaconnect.formextraction.provider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FormExtractionInput {
    byte[] imageBytes;
    String mimeType;
    String formType;
    String formVersion;
    String languageHint;
    String requestId;
}
