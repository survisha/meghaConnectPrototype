package com.survisha.meghaconnect.formextraction.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProviderResultMapper {
    private final ObjectMapper objectMapper;

    public VisitorFormExtractionResult map(JsonNode node, AIProviderType provider, String model,
                                           String providerRequestId, long durationMs) {
        if (node == null || !node.isObject() || !node.has("name") || !node.has("mobileNumber")
                || !node.has("age") || !node.has("address")) {
            throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE",
                    "AI provider returned an incomplete extraction response.", 502);
        }
        List<String> warnings = new ArrayList<>();
        node.path("warnings").forEach(value -> warnings.add(value.asText()));
        return VisitorFormExtractionResult.builder()
                .documentType(node.path("documentType").asText("VISITOR_REGISTRATION"))
                .formVersion(node.path("formVersion").asText())
                .extractedName(stringField(node.path("name")))
                .extractedMobileNumber(stringField(node.path("mobileNumber")))
                .extractedAge(integerField(node.path("age")))
                .extractedAddress(stringField(node.path("address")))
                .warnings(warnings).requiresManualReview(node.path("requiresManualReview").asBoolean(true))
                .provider(provider).model(model).providerRequestId(providerRequestId)
                .processingTimeMs(durationMs).extractionTimestamp(DateTimeUtil.nowIST()).build();
    }

    private ExtractedVisitorField<String> stringField(JsonNode node) {
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructParametricType(ExtractedVisitorField.class, String.class));
    }
    private ExtractedVisitorField<Integer> integerField(JsonNode node) {
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructParametricType(ExtractedVisitorField.class, Integer.class));
    }
}
