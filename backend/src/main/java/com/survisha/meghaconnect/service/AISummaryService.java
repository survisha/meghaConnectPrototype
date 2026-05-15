package com.survisha.meghaconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI Summary Service.
 * Currently returns a dummy/placeholder summary.
 * Structure is pluggable for local LLM integration.
 */
@Service
public class AISummaryService {

    /**
     * Generate a short summary from the uploaded document.
     * 
     * Currently: returns placeholder text based on file metadata.
     * Future: integrate with the local Ollama client for actual document analysis.
     *
     * @param file the uploaded document (PDF, DOCX, image, etc.)
     * @return a short AI-generated summary string
     */
    public String generateShortSummary(MultipartFile file) {
        // TODO: Integrate with local Ollama document analysis
        // Example integration point:
        //   return aiClient.analyze(file.getBytes(), "Summarize this document in 2 sentences");
        
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        long fileSizeKb = file.getSize() / 1024;
        
        return String.format(
            "[AI Summary – Placeholder] Document '%s' (%d KB) received. " +
            "Full AI analysis will be available after local LLM integration. " +
            "Please review the document manually for detailed information.",
            filename, fileSizeKb
        );
    }
    
    /**
     * Generate a summary from text content (e.g., agenda brief).
     */
    public String generateSummaryFromText(String applicantName, String district,
                                           String agendaType, String agendaBrief) {
        // TODO: Replace with actual LLM call
        String brief = agendaBrief != null && agendaBrief.length() > 120
            ? agendaBrief.substring(0, 120) + "…"
            : agendaBrief;
        return String.format("%s (%s) – %s: %s", applicantName, district, agendaType, brief);
    }
}
