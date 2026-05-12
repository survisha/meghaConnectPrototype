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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFileService {

    private static final Set<String> STAFF_AUTHORITIES = Set.of(
            "ROLE_ADMIN",
            "ROLE_OSD",
            "ROLE_APPROVER",
            "ROLE_CMO_OFFICER",
            "ROLE_CMO",
            "ROLE_DATA_ENTRY_OPERATOR",
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
