package com.survisha.meghaconnect.formextraction.provider;

public interface AIFormExtractionProvider {
    AIProviderType getProviderType();
    VisitorFormExtractionResult extract(FormExtractionInput input);
}
