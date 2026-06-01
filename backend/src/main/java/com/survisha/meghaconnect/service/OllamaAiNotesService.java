package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI notes service kept under its historical name for existing callers.
 * Calls are routed through the configured provider facade.
 */
@Service
@RequiredArgsConstructor
public class OllamaAiNotesService {

    private final LLMProviderService llmProviderService;

    public String generateNotes(String documentText) {
        String prompt = """
                You are assisting Meghalaya CM Office staff.
                Read the uploaded appointment/supporting document and return exactly these sections:

                Summary:
                - 3 to 5 short bullets covering the request, applicant need, location, and key dates/amounts.

                Important Details:
                - Bullet list of names, phone numbers, IDs, departments, schemes, amounts, locations, deadlines, and commitments found.

                Missing or Unclear Information:
                - Bullet list of information staff may need to verify. Write "Not found" if none.

                Risk Flags:
                - Bullet list of urgency, duplicate-looking claims, legal/safety/health issues, or conflicting information. Write "Not found" if none.

                Do not invent facts. If the document is unclear, say so.

                Document text:
                %s
                """.formatted(documentText == null ? "" : documentText);

        return llmProviderService.generateText(prompt, LlmOptions.builder()
                        .module("ai-notes")
                        .promptType("document-notes")
                        .build())
                .orElseThrow(() -> new IllegalStateException("AI service is currently unavailable."));
    }

    public String getModelName() {
        return llmProviderService.modelName();
    }
}
