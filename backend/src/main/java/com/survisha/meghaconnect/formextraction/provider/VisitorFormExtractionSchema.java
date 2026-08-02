package com.survisha.meghaconnect.formextraction.provider;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class VisitorFormExtractionSchema {
    public Map<String,Object> jsonSchema() {
        Map<String,Object> properties = new LinkedHashMap<>();
        properties.put("epic", Map.of("type",List.of("string","null")));
        properties.put("name", Map.of("type",List.of("string","null")));
        properties.put("mobile", Map.of("type",List.of("string","null")));
        properties.put("address", Map.of("type",List.of("string","null")));
        properties.put("warnings", Map.of("type","array","items",Map.of("type","string")));
        properties.put("requiresManualReview", Map.of("type","boolean"));
        return Map.of("type","object","additionalProperties",false,"properties",properties,
                "required",List.of("epic","name","mobile","address","requiresManualReview","warnings"));
    }
}
