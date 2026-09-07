package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.config.AiNotesProperties;
import com.survisha.meghaconnect.entity.DocumentUpload;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class AiNotesDocumentProcessor {
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractionService textExtractionService;
    private final AiNotesProperties properties;

    public ProcessedDocument process(DocumentUpload document) {
        long started = System.nanoTime();
        Path path = fileStorageService.resolveDocumentPath(document);
        MediaType type = fileStorageService.mediaTypeFromMetadata(document, path);
        try {
            byte[] signature = readPrefix(path, 12);
            if (isJpeg(signature) || isPng(signature)) {
                byte[] bytes = Files.readAllBytes(path);
                validateSize(bytes.length);
                ProcessedImage image = preprocess(ImageIO.read(new ByteArrayInputStream(bytes)), bytes.length);
                logDone(document, "vision-image", started, 1);
                return ProcessedDocument.vision(List.of(image.bytes()), false);
            }
            if (isPdf(signature) || MediaType.APPLICATION_PDF.includes(type)) {
                String text = extractWithoutFailing(document);
                if (isMeaningful(text)) {
                    logDone(document, "text-pdf", started, 0);
                    return ProcessedDocument.text(text);
                }
                List<byte[]> pages = renderPdf(path);
                logDone(document, "vision-pdf", started, pages.size());
                return ProcessedDocument.vision(pages, true);
            }
            String text = textExtractionService.extractText(document);
            if (!isMeaningful(text)) throw new IllegalArgumentException("No meaningful document content was found.");
            logDone(document, "text", started, 0);
            return ProcessedDocument.text(text);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read or render uploaded document.", e);
        }
    }

    boolean isMeaningful(String text) {
        if (text == null) return false;
        String compact = text.replaceAll("\\s+", "").replaceAll("[^\\p{L}\\p{N}]", "");
        return compact.length() >= Math.max(20, properties.getMinMeaningfulTextChars());
    }

    private String extractWithoutFailing(DocumentUpload document) {
        try { return textExtractionService.extractText(document); }
        catch (RuntimeException ignored) { return ""; }
    }

    private List<byte[]> renderPdf(Path path) throws IOException {
        long started = System.nanoTime();
        try (PDDocument pdf = PDDocument.load(path.toFile())) {
            if (pdf.getNumberOfPages() == 0) throw new IllegalArgumentException("PDF contains no pages.");
            int count = Math.min(pdf.getNumberOfPages(), Math.max(1, properties.getMaxPdfPages()));
            PDFRenderer renderer = new PDFRenderer(pdf);
            List<byte[]> pages = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BufferedImage rendered = renderer.renderImageWithDPI(i, properties.getRenderDpi(), ImageType.RGB);
                pages.add(preprocess(rendered, -1).bytes());
            }
            log.info("AI notes PDF rendered pages={} totalPages={} dpi={} durationMs={}", count, pdf.getNumberOfPages(), properties.getRenderDpi(), elapsed(started));
            return pages;
        }
    }

    private ProcessedImage preprocess(BufferedImage source, long originalBytes) throws IOException {
        if (source == null) throw new IllegalArgumentException("Image could not be decoded.");
        int longest = Math.max(source.getWidth(), source.getHeight());
        double scale = Math.min(1d, (double) properties.getMaxLongestSide() / longest);
        int width = Math.max(1, (int)Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int)Math.round(source.getHeight() * scale));
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(source, 0, 0, width, height, null); graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(stream); ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); params.setCompressionQuality(properties.getJpegQuality());
            writer.write(null, new IIOImage(output, null, null), params);
        } finally { writer.dispose(); }
        validateSize(bytes.size());
        log.info("AI notes image preprocessed originalWidth={} originalHeight={} processedWidth={} processedHeight={} originalBytes={} processedBytes={}", source.getWidth(), source.getHeight(), width, height, originalBytes, bytes.size());
        return new ProcessedImage(bytes.toByteArray());
    }

    private void validateSize(long size) { if (size <= 0 || size > properties.getMaxImageBytes()) throw new IllegalArgumentException("Image exceeds the configured AI notes size limit."); }
    private byte[] readPrefix(Path path, int length) throws IOException { try(InputStream in=Files.newInputStream(path)){ return in.readNBytes(length); } }
    private boolean isPdf(byte[] b){return b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-';}
    private boolean isJpeg(byte[] b){return b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;}
    private boolean isPng(byte[] b){return b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71;}
    private long elapsed(long started){return (System.nanoTime()-started)/1_000_000;}
    private void logDone(DocumentUpload d,String mode,long started,int pages){log.info("AI notes document prepared documentId={} mode={} pages={} durationMs={}",d.getId(),mode,pages,elapsed(started));}
    private record ProcessedImage(byte[] bytes) {}
    public record ProcessedDocument(String text, List<byte[]> images, boolean renderedPdf) {
        static ProcessedDocument text(String text){return new ProcessedDocument(text,List.of(),false);}
        static ProcessedDocument vision(List<byte[]> images,boolean pdf){return new ProcessedDocument(null,List.copyOf(images),pdf);}
        public boolean vision(){return !images.isEmpty();}
    }
}
