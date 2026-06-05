package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.config.OllamaAiProperties;
import com.survisha.meghaconnect.entity.DocumentUpload;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTextExtractionService {

    private final FileStorageService fileStorageService;
    private final OllamaAiProperties properties;

    public String extractText(DocumentUpload document) {
        Path documentPath = fileStorageService.resolveDocumentPath(document);
        String fileName = firstNonBlank(document.getOriginalFilename(), document.getStoredFileName(), "document-" + document.getId());
        if (isImageDocument(document, fileName)) {
            return imageDocumentContext(document, fileName);
        }
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            metadata.set(HttpHeaders.CONTENT_TYPE, document.getContentType());
        }

        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();

        try (InputStream input = Files.newInputStream(documentPath)) {
            parser.parse(input, handler, metadata, context);
            String extractedText = limit(handler.toString());
            if (extractedText.isBlank()) {
                throw new IllegalStateException("No extractable text found in uploaded document.");
            }
            log.info("Extracted AI document text requestId={} documentId={} chars={}",
                    com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(),
                    document.getId(),
                    extractedText.length());
            return extractedText;
        } catch (TikaException | SAXException | java.io.IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to extract text from uploaded document.", e);
        }
    }

    private boolean isImageDocument(DocumentUpload document, String fileName) {
        String contentType = firstNonBlank(document.getContentType(), document.getMimeType(), "");
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        return contentType.toLowerCase().startsWith("image/")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".bmp");
    }

    private String imageDocumentContext(DocumentUpload document, String fileName) {
        String documentType = firstNonBlank(document.getDocumentType(), "IMAGE_DOCUMENT");
        String contentType = firstNonBlank(document.getContentType(), document.getMimeType(), "image");
        String uploadedBy = firstNonBlank(document.getUploadedBy(), "unknown");

        return limit("""
                Image document uploaded for appointment review.
                File name: %s
                Document type: %s
                Content type: %s
                Document reference: %s
                Uploaded by: %s

                This file is an image, so plain text extraction is not available in the current runtime.
                Treat it as visual evidence or a meeting proof photo. Staff should inspect the image preview directly.
                For meeting proof photos, check whether the visitor is visible, whether a signed or stamped document is visible,
                whether date/signature/official marks are legible, and whether the image appears relevant to the appointment.
                Do not invent names, dates, signatures, amounts, or document contents unless they are explicitly available from metadata above.
                """.formatted(fileName, documentType, contentType, document.getId(), uploadedBy));
    }

    private String limit(String value) {
        String normalized = value == null ? "" : value.trim();
        int maxChars = Math.max(1000, properties.getMaxInputChars());
        return normalized.length() > maxChars ? normalized.substring(0, maxChars) : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
