package com.survisha.meghaconnect.formextraction.provider;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class VisitorFormExtractionSchema {
    public Map<String,Object> jsonSchema() {
        Map<String,Object> properties = new LinkedHashMap<>();
        properties.put("documentType", Map.of("type","string","enum",List.of("VISITOR_REGISTRATION")));
        properties.put("formVersion", Map.of("type","string"));
        properties.put("name", field(List.of("string","null")));
        properties.put("mobileNumber", field(List.of("string","null")));
        properties.put("age", field(List.of("integer","null")));
        properties.put("address", field(List.of("string","null")));
        properties.put("warnings", Map.of("type","array","items",Map.of("type","string")));
        properties.put("requiresManualReview", Map.of("type","boolean"));
        return Map.of("type","object","additionalProperties",false,"properties",properties,
                "required",List.of("documentType","formVersion","name","mobileNumber","age","address","warnings","requiresManualReview"));
    }

    private Map<String,Object> field(List<String> valueTypes) {
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("value", Map.of("type",valueTypes));
        props.put("status", Map.of("type","string","enum",List.of("EXTRACTED","NOT_FOUND","UNREADABLE","AMBIGUOUS","CROSSED_OUT")));
        props.put("confidence", Map.of("type","string","enum",List.of("HIGH","MEDIUM","LOW","NONE")));
        props.put("reason", Map.of("type",List.of("string","null")));
        return Map.of("type","object","additionalProperties",false,"properties",props,
                "required",List.of("value","status","confidence","reason"));
    }
}
