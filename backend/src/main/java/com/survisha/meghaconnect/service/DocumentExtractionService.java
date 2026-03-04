package com.survisha.meghaconnect.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service to extract plain text from uploaded documents.
 *
 * Supported formats:
 *   - PDF  (Apache PDFBox)
 *   - DOCX (Apache POI – XWPFDocument)
 *   - DOC  (Apache POI – HWPFDocument / scratchpad)
 *   - Images / other – returns a placeholder noting OCR is not available
 *     (Tesseract OCR would be integrated here when the native lib is available)
 *
 * Used by: AiDocumentService → AiController
 */
@Service
public class DocumentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionService.class);

    /**
     * Extract text content from an uploaded file.
     *
     * @param file the uploaded MultipartFile
     * @return extracted text string; never null – falls back to empty/placeholder
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";
        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase()
                : "";

        try {
            if (isPdf(filename, contentType)) {
                return extractFromPdf(file);
            } else if (isDocx(filename, contentType)) {
                return extractFromDocx(file);
            } else if (isDoc(filename, contentType)) {
                return extractFromDoc(file);
            } else if (isImage(filename, contentType)) {
                // Tesseract OCR integration point.
                // When Tesseract is available, call:
                //   Tesseract ocr = new Tesseract();
                //   return ocr.doOCR(ImageIO.read(file.getInputStream()));
                log.info("Image document received ({}); OCR not available – returning filename as hint.", filename);
                return "[Image document: " + file.getOriginalFilename() + ". OCR text extraction requires Tesseract integration.]";
            } else {
                // Try PDF as fallback for unknown types
                try {
                    return extractFromPdf(file);
                } catch (Exception e) {
                    log.debug("PDF fallback extraction failed for {}: {}", filename, e.getMessage());
                    return "[Unsupported document format: " + file.getOriginalFilename() + "]";
                }
            }
        } catch (IOException e) {
            log.warn("Failed to extract text from {}: {}", filename, e.getMessage());
            return "[Text extraction failed for: " + file.getOriginalFilename() + "]";
        }
    }

    // ── PDF ──────────────────────────────────────────────────────────────────

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            return text != null ? text.trim() : "";
        }
    }

    // ── DOCX ─────────────────────────────────────────────────────────────────

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            return text != null ? text.trim() : "";
        }
    }

    // ── DOC (legacy binary format) ───────────────────────────────────────────

    private String extractFromDoc(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            StringBuilder sb = new StringBuilder();
            for (String para : extractor.getParagraphText()) {
                sb.append(para).append("\n");
            }
            return sb.toString().trim();
        }
    }

    // ── Type helpers ──────────────────────────────────────────────────────────

    private boolean isPdf(String name, String ct) {
        return name.endsWith(".pdf") || ct.contains("pdf");
    }

    private boolean isDocx(String name, String ct) {
        return name.endsWith(".docx")
                || ct.contains("openxmlformats-officedocument.wordprocessingml");
    }

    private boolean isDoc(String name, String ct) {
        return name.endsWith(".doc") || ct.contains("msword");
    }

    private boolean isImage(String name, String ct) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".tiff")
                || ct.startsWith("image/");
    }
}
