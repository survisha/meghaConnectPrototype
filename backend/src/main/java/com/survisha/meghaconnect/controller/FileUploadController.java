package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.DocumentUploadRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.service.AISummaryService;
import com.survisha.meghaconnect.service.AppointmentDocumentAiNotesService;
import com.survisha.meghaconnect.service.DocumentFileService;
import com.survisha.meghaconnect.service.FileStorageService;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * File upload and authenticated document streaming API.
 *
 * The UI receives only document ids and file metadata. Server file paths stay encrypted
 * in the database and are never returned by these endpoints.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload, preview, and download operations")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final DocumentFileService documentFileService;
    private final AISummaryService aiSummaryService;
    private final AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    private final DocumentUploadRepository documentUploadRepository;
    private final VisitorRepository visitorRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Upload a file for a visitor/application and persist document metadata.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document and return secure document metadata")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("visitorId") Long visitorId,
            @RequestParam("applicationId") String applicationId,
            @RequestParam(value = "documentType", defaultValue = "GENERAL") String documentType,
            @RequestParam(value = "generateSummary", defaultValue = "false") boolean generateSummary,
            Authentication authentication) {

        try {
            FileStorageService.StoredFileMetadata storedFile =
                    fileStorageService.storeFileSecure(file, visitorId, applicationId);
            DocumentUpload document = saveDocumentMetadata(storedFile, visitorId, applicationId, documentType, authentication);
            queueAiNotes(document);

            String summary = generateSummary ? aiSummaryService.generateShortSummary(file) : "";

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", document.getId());
            response.put("originalFileName", document.getOriginalFilename());
            response.put("storedFileName", document.getStoredFileName());
            response.put("contentType", document.getContentType());
            response.put("fileSize", document.getFileSizeBytes());
            response.put("previewUrl", "/api/files/preview/" + document.getId());
            response.put("downloadUrl", "/api/files/download/" + document.getId());
            response.put("visitorId", visitorId);
            response.put("applicationId", applicationId);
            response.put("summary", summary);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File storage error.");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/preview/{id}")
    @Operation(summary = "Preview an uploaded document by id")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id, Authentication authentication) {
        DocumentFileService.StoredDocumentResource document =
                documentFileService.loadDocument(id, authentication, true);
        return streamDocument(document, true);
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "Download an uploaded document by id")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication) {
        DocumentFileService.StoredDocumentResource document =
                documentFileService.loadDocument(id, authentication, false);
        return streamDocument(document, false);
    }

    private DocumentUpload saveDocumentMetadata(FileStorageService.StoredFileMetadata storedFile,
                                                Long visitorId,
                                                String applicationId,
                                                String documentType,
                                                Authentication authentication) {
        Visitor visitor = visitorRepository.findById(visitorId).orElse(null);
        Appointment appointment = appointmentRepository.findByApplicationId(applicationId).orElse(null);
        LocalDateTime now = DateTimeUtil.nowIST();
        String uploadedBy = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "visitor_" + visitorId;

        DocumentUpload document = DocumentUpload.builder()
                .visitor(visitor)
                .appointment(appointment)
                .documentType(documentType)
                .originalFilename(storedFile.getOriginalFileName())
                .storedFileName(storedFile.getStoredFileName())
                .filePath(storedFile.getEncryptedFilePath())
                .encryptedFilePath(storedFile.getEncryptedFilePath())
                .secureHash(storedFile.getSecureHash())
                .fileSizeBytes(storedFile.getFileSize())
                .mimeType(storedFile.getContentType())
                .contentType(storedFile.getContentType())
                .uploadedBy(uploadedBy)
                .uploadedDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return documentUploadRepository.save(document);
    }

    private void queueAiNotes(DocumentUpload document) {
        try {
            appointmentDocumentAiNotesService.queueGeneration(document);
        } catch (Exception e) {
            log.warn("Unable to queue AI notes requestId={} appointmentId={} documentId={}",
                    RequestContextUtil.getRequestId(),
                    document.getAppointment() != null ? document.getAppointment().getId() : null,
                    document.getId(),
                    e);
        }
    }

    private ResponseEntity<Resource> streamDocument(DocumentFileService.StoredDocumentResource document,
                                                    boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(sanitizeHeaderFilename(document.getOriginalFileName()), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(document.getMediaType())
                .contentLength(document.getContentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.getResource());
    }

    private String sanitizeHeaderFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
