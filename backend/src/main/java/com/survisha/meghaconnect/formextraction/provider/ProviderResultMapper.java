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
        if (node == null || !node.isObject() || !node.has("epic") || !node.has("name") || !node.has("mobile") || !node.has("address")) {
            throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE",
                    "AI provider returned an incomplete extraction response.", 502);
        }
        List<String> warnings = new ArrayList<>();
        node.path("warnings").forEach(value -> warnings.add(value.asText()));
        return VisitorFormExtractionResult.builder()
                .documentType("VISITOR_REGISTRATION").formVersion("")
                .extractedEpic(field(node.get("epic"))).extractedName(field(node.get("name")))
                .extractedMobileNumber(field(node.get("mobile"))).extractedAddress(field(node.get("address")))
                .warnings(warnings).requiresManualReview(node.path("requiresManualReview").asBoolean(true))
                .provider(provider).model(model).providerRequestId(providerRequestId)
                .processingTimeMs(durationMs).extractionTimestamp(DateTimeUtil.nowIST()).build();
    }

    private ExtractedVisitorField<String> field(JsonNode node) {
        ExtractedVisitorField<String> field=new ExtractedVisitorField<>();
        String value=node==null||node.isNull()||node.asText().isBlank()?null:node.asText();
        field.setValue(value); field.setStatus(value==null?ExtractedVisitorField.FieldStatus.NOT_FOUND:ExtractedVisitorField.FieldStatus.EXTRACTED);
        field.setConfidence(value==null?ExtractedVisitorField.Confidence.NONE:ExtractedVisitorField.Confidence.HIGH);
        return field;
    }
}
