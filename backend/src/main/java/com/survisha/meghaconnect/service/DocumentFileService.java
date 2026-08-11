package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.DocumentUploadRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFileService {

    private static final Set<String> STAFF_AUTHORITIES = Set.of(
            "ROLE_ADMIN",
            "ROLE_APPROVER",
            "ROLE_DEO",
            "ROLE_HCM"
    );

    private static final Set<String> SUPPORTED_DOWNLOAD_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final DocumentUploadRepository documentUploadRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractionService documentTextExtractionService;

    @Value("${document.preview.conversion.enabled:false}")
    private boolean documentPreviewConversionEnabled;

    @Value("${document.preview.libreoffice.path:soffice}")
    private String libreOfficePath;

    @Value("${document.preview.cache-dir:${java.io.tmpdir}/meghaconnect-preview-cache}")
    private String previewCacheDir;

    @Value("${document.preview.conversion.timeout-seconds:30}")
    private long previewConversionTimeoutSeconds;

    @Transactional(readOnly = true)
    public StoredDocumentResource loadDocument(Long documentId, Authentication authentication, boolean preview) {
        DocumentUpload document = documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.CONTENT_NOT_FOUND,
                        "Document not found.",
                        404
                ));

        assertCanAccess(document, authentication);

        Path filePath = fileStorageService.resolveDocumentPath(document);
        MediaType mediaType = fileStorageService.mediaTypeFromMetadata(document, filePath);
        if (!isSupportedDownloadType(mediaType)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.INVALID_CONTENT_TYPE,
                    "Document type is not supported.",
                    415
            );
        }
        if (preview && isWordDocument(mediaType, filePath)) {
            return convertWordDocumentForPreview(documentId, document, filePath);
        }
        if (preview && !isPreviewable(mediaType)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.INVALID_CONTENT_TYPE,
                    "Inline preview is not supported for this document type.",
                    415
            );
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return new StoredDocumentResource(
                    resource,
                    firstNonBlank(document.getOriginalFilename(), document.getStoredFileName(), "document-" + documentId),
                    mediaType,
                    Files.size(filePath)
            );
        } catch (MalformedURLException e) {
            log.warn("Stored document URI is invalid documentId={}", documentId);
            throw new MeghaConnectException(
                    ErrorCodeConstants.CONTENT_NOT_FOUND,
                    "Document file is unavailable.",
                    404,
                    e
            );
        } catch (IOException e) {
            log.warn("Stored document size could not be read documentId={}", documentId);
            throw new MeghaConnectException(
                    ErrorCodeConstants.CONTENT_NOT_FOUND,
                    "Document file is unavailable.",
                    404,
                    e
            );
        }
    }

    private void assertCanAccess(DocumentUpload document, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.USER_NOT_AUTHENTICATED,
                    ErrorCodeConstants.USER_NOT_AUTHENTICATED_MSG,
                    401
            );
        }

        if (hasStaffAuthority(authentication)) {
            return;
        }

        Long authenticatedVisitorId = parseVisitorId(authentication.getName());
        if (authenticatedVisitorId != null && authenticatedVisitorId.equals(resolveDocumentVisitorId(document))) {
            return;
        }

        throw new MeghaConnectException(
                ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG,
                403
        );
    }

    private boolean hasStaffAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_AUTHORITIES::contains);
    }

    private Long resolveDocumentVisitorId(DocumentUpload document) {
        Visitor visitor = document.getVisitor();
        if (visitor != null) {
            return visitor.getId();
        }

        Appointment appointment = document.getAppointment();
        if (appointment != null && appointment.getApplicant() != null) {
            return appointment.getApplicant().getId();
        }
        return null;
    }

    private Long parseVisitorId(String username) {
        if (username == null || !username.startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(username.substring("visitor_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isSupportedDownloadType(MediaType mediaType) {
        return mediaType != null
                && ("image".equalsIgnoreCase(mediaType.getType())
                || SUPPORTED_DOWNLOAD_TYPES.contains(mediaType.toString().toLowerCase()));
    }

    private boolean isPreviewable(MediaType mediaType) {
        return mediaType != null
                && ("image".equalsIgnoreCase(mediaType.getType()) || MediaType.APPLICATION_PDF.includes(mediaType));
    }

    private boolean isWordDocument(MediaType mediaType, Path filePath) {
        String type = mediaType != null ? mediaType.toString().toLowerCase(Locale.ROOT) : "";
        String name = filePath != null && filePath.getFileName() != null
                ? filePath.getFileName().toString().toLowerCase(Locale.ROOT)
                : "";
        return "application/msword".equals(type)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(type)
                || name.endsWith(".doc")
                || name.endsWith(".docx");
    }

    private StoredDocumentResource convertWordDocumentForPreview(Long documentId,
                                                                 DocumentUpload document,
                                                                 Path filePath) {
        try {
            Path cacheRoot = Paths.get(previewCacheDir).toAbsolutePath().normalize();
            Files.createDirectories(cacheRoot);
            String sourceName = filePath.getFileName().toString();
            String baseName = sourceName.replaceFirst("(?i)\\.(docx?|rtf)$", "");
            Path convertedPdf = cacheRoot.resolve(documentId + "-" + baseName + ".pdf").normalize();
            Path textFallbackPdf = cacheRoot.resolve(documentId + "-" + baseName + "-text-preview.pdf").normalize();
            if (!convertedPdf.startsWith(cacheRoot)) {
                throw new IOException("Resolved preview cache path is outside configured cache directory.");
            }
            if (!textFallbackPdf.startsWith(cacheRoot)) {
                throw new IOException("Resolved preview fallback path is outside configured cache directory.");
            }

            if (!Files.exists(convertedPdf) || Files.getLastModifiedTime(convertedPdf).compareTo(Files.getLastModifiedTime(filePath)) < 0) {
                if (documentPreviewConversionEnabled) {
                    String converter = findLibreOfficeExecutable();
                    if (converter == null) {
                        log.info("LibreOffice preview converter not found documentId={} requestId={}; using text PDF fallback",
                                documentId,
                                com.survisha.meghaconnect.util.RequestContextUtil.getRequestId());
                        createTextPdfPreview(document, textFallbackPdf);
                        convertedPdf = textFallbackPdf;
                    } else {
                        try {
                            runLibreOfficeConversion(converter, filePath, cacheRoot);
                            Path libreOfficeOutput = cacheRoot.resolve(baseName + ".pdf").normalize();
                            if (!Files.exists(libreOfficeOutput)) {
                                throw new IOException("LibreOffice did not create a PDF preview.");
                            }
                            Files.move(libreOfficeOutput, convertedPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException | InterruptedException e) {
                            if (e instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                            }
                            log.warn("LibreOffice preview conversion failed documentId={} requestId={}; using text PDF fallback: {}",
                                    documentId,
                                    com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(),
                                    e.getMessage());
                            createTextPdfPreview(document, textFallbackPdf);
                            convertedPdf = textFallbackPdf;
                        }
                    }
                } else {
                    createTextPdfPreview(document, textFallbackPdf);
                    convertedPdf = textFallbackPdf;
                }
            }

            return new StoredDocumentResource(
                    new UrlResource(convertedPdf.toUri()),
                    firstNonBlank(document.getOriginalFilename(), document.getStoredFileName(), "document-" + documentId)
                            .replaceFirst("(?i)\\.docx?$", ".pdf"),
                    MediaType.APPLICATION_PDF,
                    Files.size(convertedPdf)
            );
        } catch (IOException e) {
            throw previewConversionException(e);
        }
    }

    private void runLibreOfficeConversion(String converter, Path filePath, Path outDir) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                converter,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                outDir.toString(),
                filePath.toString()
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(Duration.ofSeconds(previewConversionTimeoutSeconds).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Document preview conversion timed out.");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Document preview conversion failed.");
        }
    }

    private String findLibreOfficeExecutable() {
        String configured = firstNonBlank(libreOfficePath, "soffice");
        return Arrays.stream(new String[] {
                configured,
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
        })
                .filter(value -> value != null && !value.isBlank())
                .filter(this::canStartExecutable)
                .findFirst()
                .orElse(null);
    }

    private boolean canStartExecutable(String executable) {
        if (executable.contains("\\") || executable.contains("/") || executable.endsWith(".exe")) {
            return Files.isRegularFile(Paths.get(executable));
        }
        try {
            Process process = new ProcessBuilder(executable, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void createTextPdfPreview(DocumentUpload document, Path outputPath) throws IOException {
        String text;
        try {
            text = documentTextExtractionService.extractText(document);
        } catch (Exception e) {
            text = "Preview conversion is unavailable and readable text could not be extracted from this document.\n\n"
                    + "Please download the original document to view it.";
        }
        if (text == null || text.isBlank()) {
            text = "No readable text was extracted from this document. Please download the original document to view it.";
        }

        Files.createDirectories(outputPath.getParent());
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDPageContentStream content = new PDPageContentStream(pdf, page);
            try {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.setLeading(15);
                content.newLineAtOffset(48, page.getMediaBox().getHeight() - 56);
                int lineCount = 0;
                for (String line : wrapPreviewText(text)) {
                    if (lineCount >= 48) {
                        content.endText();
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        pdf.addPage(page);
                        content = new PDPageContentStream(pdf, page);
                        content.beginText();
                        content.setFont(PDType1Font.HELVETICA, 11);
                        content.setLeading(15);
                        content.newLineAtOffset(48, page.getMediaBox().getHeight() - 56);
                        lineCount = 0;
                    }
                    content.showText(line);
                    content.newLine();
                    lineCount++;
                }
                content.endText();
            } finally {
                content.close();
            }
            try (OutputStream output = Files.newOutputStream(outputPath)) {
                pdf.save(output);
            }
        }
    }

    private java.util.List<String> wrapPreviewText(String text) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String rawLine : text.replace("\r", "").split("\n")) {
            String remaining = rawLine.trim();
            if (remaining.isEmpty()) {
                lines.add(" ");
                continue;
            }
            while (remaining.length() > 92) {
                int breakAt = remaining.lastIndexOf(' ', 92);
                if (breakAt < 40) {
                    breakAt = 92;
                }
                lines.add(sanitizePdfText(remaining.substring(0, breakAt).trim()));
                remaining = remaining.substring(breakAt).trim();
            }
            lines.add(sanitizePdfText(remaining));
        }
        return lines;
    }

    private String sanitizePdfText(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private MeghaConnectException previewConversionException(Exception e) {
        log.warn("Document preview conversion failed requestId={}", com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(), e);
        return new MeghaConnectException(
                ErrorCodeConstants.INVALID_CONTENT_TYPE,
                "Preview not available. Please download the document to view.",
                415,
                e
        );
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

    @Getter
    public static class StoredDocumentResource {
        private final Resource resource;
        private final String originalFileName;
        private final MediaType mediaType;
        private final long contentLength;

        private StoredDocumentResource(Resource resource,
                                       String originalFileName,
                                       MediaType mediaType,
                                       long contentLength) {
            this.resource = resource;
            this.originalFileName = originalFileName;
            this.mediaType = mediaType;
            this.contentLength = contentLength;
        }
    }
}
