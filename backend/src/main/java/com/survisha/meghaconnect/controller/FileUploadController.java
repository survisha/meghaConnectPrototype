package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.AISummaryService;
import com.survisha.meghaconnect.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * File upload/download API.
 * POST   /api/files/upload
 * GET    /api/files/download/{id}
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final AISummaryService aiSummaryService;

    /**
     * Upload a file for a given visitorId and applicationId.
     * Optional: generateSummary flag triggers AI summary generation.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("visitorId") Long visitorId,
            @RequestParam("applicationId") String applicationId,
            @RequestParam(value = "generateSummary", defaultValue = "false") boolean generateSummary) {

        try {
            String storedPath = fileStorageService.storeFile(file, visitorId, applicationId);
            String summary = null;
            if (generateSummary) {
                summary = aiSummaryService.generateShortSummary(file);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", storedPath);
            response.put("visitorId", visitorId);
            response.put("applicationId", applicationId);
            response.put("summary", summary != null ? summary : "");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File storage error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Download a file by its relative path (base64 encoded or URL-safe encoded path).
     * For simplicity, the path is passed as a path variable.
     */
    @GetMapping("/download/{visitorId}/{applicationId}/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long visitorId,
            @PathVariable String applicationId,
            @PathVariable String filename) {

        try {
            String relativePath = visitorId + "/" + applicationId + "/" + filename;
            Path filePath = fileStorageService.resolveFilePath(relativePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
