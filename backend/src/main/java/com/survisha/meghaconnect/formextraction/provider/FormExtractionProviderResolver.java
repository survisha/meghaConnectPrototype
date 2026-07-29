package com.survisha.meghaconnect.formextraction.provider;

import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FormExtractionProviderResolver {
    private final Map<AIProviderType, AIFormExtractionProvider> providers = new EnumMap<>(AIProviderType.class);

    public FormExtractionProviderResolver(List<AIFormExtractionProvider> implementations) {
        for (AIFormExtractionProvider provider : implementations) {
            if (providers.put(provider.getProviderType(), provider) != null) {
                throw new IllegalStateException("Duplicate form extraction provider: " + provider.getProviderType());
            }
        }
    }

    public AIFormExtractionProvider resolve(AIProviderType type) {
        if (type == null) throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_MISSING", "Form extraction provider is not configured.", 500);
        AIFormExtractionProvider provider = providers.get(type);
        if (provider == null) throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNSUPPORTED", "Configured form extraction provider is unsupported.", 500);
        return provider;
    }
}
