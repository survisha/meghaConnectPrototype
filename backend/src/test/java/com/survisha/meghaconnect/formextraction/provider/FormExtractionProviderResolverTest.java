package com.survisha.meghaconnect.formextraction.provider;

import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FormExtractionProviderResolverTest {
    @Test void resolvesConfiguredImplementations() {
        AIFormExtractionProvider ollama = provider(AIProviderType.OLLAMA);
        AIFormExtractionProvider openai = provider(AIProviderType.OPENAI);
        FormExtractionProviderResolver resolver = new FormExtractionProviderResolver(List.of(ollama, openai));
        assertSame(ollama, resolver.resolve(AIProviderType.OLLAMA));
        assertSame(openai, resolver.resolve(AIProviderType.OPENAI));
    }

    @Test void rejectsMissingProviderImplementation() {
        FormExtractionProviderResolver resolver = new FormExtractionProviderResolver(List.of(provider(AIProviderType.OLLAMA)));
        assertThrows(FormExtractionException.class, () -> resolver.resolve(AIProviderType.OPENAI));
    }

    @Test void rejectsBlankProviderConfiguration() {
        FormExtractionProviderResolver resolver = new FormExtractionProviderResolver(List.of());
        assertThrows(FormExtractionException.class, () -> resolver.resolve(null));
    }

    @Test void rejectsDuplicateProviderTypes() {
        assertThrows(IllegalStateException.class, () -> new FormExtractionProviderResolver(
                List.of(provider(AIProviderType.OLLAMA), provider(AIProviderType.OLLAMA))));
    }

    private AIFormExtractionProvider provider(AIProviderType type) {
        return new AIFormExtractionProvider() {
            public AIProviderType getProviderType() { return type; }
            public VisitorFormExtractionResult extract(FormExtractionInput input) { return null; }
        };
    }
}
