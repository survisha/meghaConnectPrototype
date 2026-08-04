package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.exception.VisitorRegistrationValidationException;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.survisha.meghaconnect.monitoring.MonitoredOperation;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:image/(jpeg|jpg|png);base64,(.+)$", Pattern.CASE_INSENSITIVE);
    private static final String ENCRYPTED_PATH_PREFIX = "enc:";

    private final FilePathCryptoService filePathCryptoService;
    private final MalwareScanService malwareScanService;

    @Value("${file.upload.root-path:${meghaconnect.storage.base-path:/uploads}}")
    private String basePath;

    @Value("${meghaconnect.storage.visitor-photo-path:visitor-photos}")
    private String visitorPhotoPath;

    @Value("${meghaconnect.storage.allowed-types:pdf,jpg,jpeg,png,doc,docx}")
    private String allowedTypes;

    @Value("${meghaconnect.storage.max-file-size-mb:10}")
    private long maxFileSizeMb;

    /**
     * Legacy helper retained for callers that only need the generated relative key.
     * New document metadata writes should use storeFileSecure.
     */
    public String storeFile(MultipartFile file, Long visitorId, String applicationId) throws IOException {
        return storeFileSecure(file, visitorId, applicationId).getRelativePath();
    }

    /**
     * Stores a document as normal binary data and returns encrypted metadata for DB persistence.
     */
    @MonitoredOperation("file_upload")
    public StoredFileMetadata storeFileSecure(MultipartFile file, Long visitorId, String applicationId) throws IOException {
        validateFile(file);
        malwareScanService.assertSafe(file);
        validatePathSegment(visitorId != null ? visitorId.toString() : null, "visitorId");
        validatePathSegment(applicationId, "applicationId");

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension.toLowerCase();

        String relativePath = visitorId + "/" + applicationId + "/" + filename;
        Path targetDir = Paths.get(basePath, visitorId.toString(), applicationId);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

        String encryptedPath = filePathCryptoService.encryptPath(relativePath);
        String secureHash = filePathCryptoService.hashPath(relativePath);
        String contentType = resolveContentType(filename, file.getContentType());

        return new StoredFileMetadata(
                originalFilename,
                filename,
                contentType,
                file.getSize(),
                relativePath,
                encryptedPath,
                secureHash
        );
    }

    /**
     * Stores a browser-captured live photo outside the database and returns the
     * relative path/key to persist with the visitor row.
     */
    public String storeVisitorPhotoBase64(String livePhotoBase64) throws IOException {
        String requestId = RequestContextUtil.getRequestId();
        DecodedImage decoded = decodeAndValidateImage(livePhotoBase64);

        String datePath = DateTimeUtil.currentDateIST().toString();
        String safeRequestId = requestId.replaceAll("[^A-Za-z0-9._-]", "-");
        String filename = safeRequestId + "-" + UUID.randomUUID() + "." + decoded.extension;
        malwareScanService.assertSafe(decoded.bytes, filename, mediaTypeFromExtension(filename).toString());
        String relativePath = visitorPhotoPath + "/" + datePath + "/" + filename;

        Path targetDir = Paths.get(basePath, visitorPhotoPath, datePath);
        Files.createDirectories(targetDir);
        Files.write(targetDir.resolve(filename), decoded.bytes);

        log.info("Stored visitor live photo requestId={} path={} sizeBytes={}", requestId, relativePath, decoded.bytes.length);
        return relativePath;
    }

    public Path resolveFilePath(String relativePath) {
        return resolveSafeRelativePath(relativePath);
    }

    public Path resolveDocumentPath(DocumentUpload document) {
        if (document == null) {
            throw documentUnavailable();
        }

        String relativePath = resolveStoredRelativePath(document);
        if (document.getSecureHash() != null
                && !document.getSecureHash().isBlank()
                && !filePathCryptoService.verifyPathHash(relativePath, document.getSecureHash())) {
            log.warn("Rejected document file because stored path hash did not match requestId={}",
                    RequestContextUtil.getRequestId());
            throw documentUnavailable();
        }

        Path target = resolveSafeRelativePath(relativePath);
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            log.warn("Stored document file is unavailable requestId={} documentId={}",
                    RequestContextUtil.getRequestId(), document.getId());
            throw documentUnavailable();
        }
        return target;
    }

    /**
     * Loads a stored visitor photo as a browser-renderable data URI. The DB
     * stores only the relative path; this method keeps reads inside basePath.
     */
    public Optional<String> loadImageDataUri(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return Optional.empty();
        }

        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath.trim()).normalize();
        if (!target.startsWith(root)) {
            log.warn("Rejected stored photo path outside upload root requestId={}", RequestContextUtil.getRequestId());
            return Optional.empty();
        }

        try {
            if (!Files.isRegularFile(target)) {
                log.warn("Stored visitor photo file not found requestId={} path={}", RequestContextUtil.getRequestId(), relativePath);
                return Optional.empty();
            }

            long maxBytes = maxFileSizeMb * 1024 * 1024;
            long fileSize = Files.size(target);
            if (fileSize <= 0 || fileSize > maxBytes) {
                log.warn("Stored visitor photo has invalid size requestId={} sizeBytes={}", RequestContextUtil.getRequestId(), fileSize);
                return Optional.empty();
            }

            byte[] bytes = Files.readAllBytes(target);
            String extension = detectImageExtension(bytes);
            if (extension.isEmpty()) {
                log.warn("Stored visitor photo is not a supported image requestId={}", RequestContextUtil.getRequestId());
                return Optional.empty();
            }

            String mimeType = "jpg".equals(extension) ? "jpeg" : extension;
            return Optional.of("data:image/" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes));
        } catch (IOException e) {
            log.warn("Unable to load stored visitor photo requestId={} path={}", RequestContextUtil.getRequestId(), relativePath, e);
            return Optional.empty();
        }
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

    private Path resolveSafeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw documentUnavailable();
        }

        String normalizedRelative = relativePath.trim().replace('\\', '/');
        if (normalizedRelative.startsWith("/") || normalizedRelative.contains("../") || normalizedRelative.contains("..")) {
            log.warn("Rejected unsafe stored document path requestId={}", RequestContextUtil.getRequestId());
            throw documentUnavailable();
        }

        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = root.resolve(normalizedRelative).normalize();
        if (!target.startsWith(root)) {
            log.warn("Rejected stored document path outside upload root requestId={}", RequestContextUtil.getRequestId());
            throw documentUnavailable();
        }
        return target;
    }

    private String resolveStoredRelativePath(DocumentUpload document) {
        String encryptedPath = trimToNull(document.getEncryptedFilePath());
        if (encryptedPath != null) {
            return filePathCryptoService.decryptPath(encryptedPath);
        }

        String storedPath = trimToNull(document.getFilePath());
        if (storedPath == null) {
            throw documentUnavailable();
        }
        if (storedPath.startsWith(ENCRYPTED_PATH_PREFIX)) {
            return filePathCryptoService.decryptPath(storedPath);
        }
        return storedPath;
    }

    private String resolveContentType(String filename, String declaredContentType) {
        String contentType = trimToNull(declaredContentType);
        if (contentType != null && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
            return contentType;
        }
        return mediaTypeFromExtension(filename).toString();
    }

    public MediaType mediaTypeFromMetadata(DocumentUpload document, Path filePath) {
        String contentType = trimToNull(document.getContentType());
        if (contentType == null) {
            contentType = trimToNull(document.getMimeType());
        }
        if (contentType == null || MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
            contentType = probeContentType(filePath);
        }
        MediaType mediaType = parseMediaType(contentType);
        if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
            mediaType = mediaTypeFromExtension(firstNonBlank(
                    document.getStoredFileName(),
                    document.getOriginalFilename(),
                    filePath != null && filePath.getFileName() != null ? filePath.getFileName().toString() : null
            ));
        }
        return mediaType;
    }

    private String probeContentType(Path filePath) {
        try {
            return filePath != null ? Files.probeContentType(filePath) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private MediaType mediaTypeFromExtension(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN;
        }
        if (lower.endsWith(".doc")) {
            return MediaType.parseMediaType("application/msword");
        }
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (lower.endsWith(".xls")) {
            return MediaType.parseMediaType("application/vnd.ms-excel");
        }
        if (lower.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private void validatePathSegment(String value, String fieldName) {
        if (value == null
                || value.isBlank()
                || value.contains("/")
                || value.contains("\\")
                || value.contains("..")) {
            throw new IllegalArgumentException(fieldName + " contains an unsafe path segment.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private MeghaConnectException documentUnavailable() {
        return new MeghaConnectException(
                ErrorCodeConstants.CONTENT_NOT_FOUND,
                "Document file is unavailable.",
                404
        );
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

    @Getter
    public static class StoredFileMetadata {
        private final String originalFileName;
        private final String storedFileName;
        private final String contentType;
        private final Long fileSize;
        private final String relativePath;
        private final String encryptedFilePath;
        private final String secureHash;

        private StoredFileMetadata(String originalFileName,
                                   String storedFileName,
                                   String contentType,
                                   Long fileSize,
                                   String relativePath,
                                   String encryptedFilePath,
                                   String secureHash) {
            this.originalFileName = originalFileName;
            this.storedFileName = storedFileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.relativePath = relativePath;
            this.encryptedFilePath = encryptedFilePath;
            this.secureHash = secureHash;
        }
    }
}
