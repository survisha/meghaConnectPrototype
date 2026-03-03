package com.survisha.meghaconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${meghaconnect.storage.base-path:/uploads}")
    private String basePath;

    @Value("${meghaconnect.storage.allowed-types:pdf,jpg,jpeg,png,doc,docx}")
    private String allowedTypes;

    @Value("${meghaconnect.storage.max-file-size-mb:10}")
    private long maxFileSizeMb;

    /**
     * Store file under /uploads/{visitorId}/{applicationId}/
     * Returns the relative path stored in DB.
     */
    public String storeFile(MultipartFile file, Long visitorId, String applicationId) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;

        String relativePath = visitorId + "/" + applicationId + "/" + filename;
        Path targetDir = Paths.get(basePath, visitorId.toString(), applicationId);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

        return relativePath;
    }

    public Path resolveFilePath(String relativePath) {
        return Paths.get(basePath).resolve(relativePath);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds " + maxFileSizeMb + " MB limit.");
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("File type '" + ext + "' is not allowed. Allowed: " + allowedTypes);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
