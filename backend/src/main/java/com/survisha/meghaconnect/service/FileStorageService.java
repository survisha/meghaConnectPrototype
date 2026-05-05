package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.VisitorRegistrationValidationException;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileStorageService {

    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:image/(jpeg|jpg|png);base64,(.+)$", Pattern.CASE_INSENSITIVE);

    @Value("${meghaconnect.storage.base-path:/uploads}")
    private String basePath;

    @Value("${meghaconnect.storage.visitor-photo-path:visitor-photos}")
    private String visitorPhotoPath;

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

    /**
     * Stores a browser-captured live photo outside the database and returns the
     * relative path/key to persist with the visitor row.
     */
    public String storeVisitorPhotoBase64(String livePhotoBase64) throws IOException {
        String requestId = RequestContextUtil.getRequestId();
        DecodedImage decoded = decodeAndValidateImage(livePhotoBase64);

        String datePath = LocalDate.now().toString();
        String safeRequestId = requestId.replaceAll("[^A-Za-z0-9._-]", "-");
        String filename = safeRequestId + "-" + UUID.randomUUID() + "." + decoded.extension;
        String relativePath = visitorPhotoPath + "/" + datePath + "/" + filename;

        Path targetDir = Paths.get(basePath, visitorPhotoPath, datePath);
        Files.createDirectories(targetDir);
        Files.write(targetDir.resolve(filename), decoded.bytes);

        log.info("Stored visitor live photo requestId={} path={} sizeBytes={}", requestId, relativePath, decoded.bytes.length);
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

    private DecodedImage decodeAndValidateImage(String livePhotoBase64) {
        if (livePhotoBase64 == null || livePhotoBase64.trim().isEmpty()) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_IMAGE_FORMAT,
                    "Captured image is required."
            );
        }

        String payload = livePhotoBase64.trim();
        String declaredExtension = "";
        Matcher matcher = DATA_URI_PATTERN.matcher(payload);
        if (matcher.matches()) {
            declaredExtension = normalizeImageExtension(matcher.group(1));
            payload = matcher.group(2);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_IMAGE_FORMAT,
                    "Captured image must be a valid base64 encoded image."
            );
        }

        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.FILE_SIZE_EXCEEDED,
                    ErrorCodeConstants.format(ErrorCodeConstants.FILE_SIZE_EXCEEDED_MSG, maxFileSizeMb)
            );
        }

        String detectedExtension = detectImageExtension(bytes);
        String extension = !declaredExtension.isEmpty() ? declaredExtension : detectedExtension;
        if (extension.isEmpty() || (!declaredExtension.isEmpty() && !declaredExtension.equals(detectedExtension))) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_IMAGE_FORMAT,
                    "Captured image must be a JPEG or PNG image."
            );
        }

        List<String> allowed = Arrays.asList(allowedTypes.toLowerCase().split(","));
        if (!allowed.contains(extension)) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_FILE_TYPE,
                    ErrorCodeConstants.format(ErrorCodeConstants.INVALID_FILE_TYPE_MSG, "jpg,jpeg,png")
            );
        }

        return new DecodedImage(bytes, extension);
    }

    private String normalizeImageExtension(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase();
        return "jpeg".equals(normalized) ? "jpg" : normalized;
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "png";
        }
        return "";
    }

    private static class DecodedImage {
        private final byte[] bytes;
        private final String extension;

        private DecodedImage(byte[] bytes, String extension) {
            this.bytes = bytes;
            this.extension = extension;
        }
    }
}
