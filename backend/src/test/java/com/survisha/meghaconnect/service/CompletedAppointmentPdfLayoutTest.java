package com.survisha.meghaconnect.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletedAppointmentPdfLayoutTest {
    @Test
    void sectionHeadersRemainWithInitialContentAcrossPages() throws Exception {
        Path output = Path.of("target", "completed-appointment-layout-sample.pdf");
        Files.createDirectories(output.getParent());
        try (PDDocument pdf = new PDDocument()) {
            ExecutiveAppointmentReportService.PdfWriter writer = new ExecutiveAppointmentReportService.PdfWriter(pdf);
            writer.title("MEGHACONNECT", "COMPLETED APPOINTMENT REPORT");
            for (int section = 1; section <= 12; section++) {
                writer.heading("SECTION " + section);
                writer.paragraph("Initial content for section " + section + ". This verifies aligned rules and safe page breaks.");
                for (int line = 0; line < 4; line++) writer.paragraph("Additional report content line " + line + " for visual layout verification.");
            }
            writer.close();
            writer.addPageNumbers();
            pdf.save(output.toFile());
        }
        try (PDDocument rendered = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(rendered);
            assertTrue(rendered.getNumberOfPages() > 1);
            assertTrue(text.contains("SECTION 1"));
            assertTrue(text.contains("Initial content for section 12"));
        }
    }
}
