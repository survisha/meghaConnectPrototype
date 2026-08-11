package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AssociateMapping;
import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.DocumentUploadRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.service.AppointmentDocumentAiNotesService;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.service.FileStorageService;
import com.survisha.meghaconnect.service.RequestValidationService;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import com.survisha.meghaconnect.util.ValidationConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Public-facing appointment booking endpoint for logged-in visitors (citizens).
 *
 * Visitor appointment submissions require a visitor JWT or authorized staff JWT.
 *
 * Flow:
 *   POST /api/v1/visitor/appointments  – submit a new appointment request
 *
 * The visitor must be registered and their applicantId (person.id) must be sent.
 * If no applicantId is provided (e.g. anonymous submission), a lightweight Person
 * record is created from the form data so that the appointment can be linked.
 */
@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
@Tag(name = "Visitor Appointments", description = "Public visitor appointment booking and workflow operations")
@Slf4j
public class VisitorAppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final VisitorRepository      visitorRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final AuditLogService       auditLogService;
    private final FileStorageService    fileStorageService;
    private final AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    private final ObjectMapper          objectMapper;
    private final RequestValidationService validationService;

    /**
     * Submit a new appointment / scheme application request from a citizen.
     *
     * Accepts multipart/form-data:
     * - applicantId: optional long
     * - applicantName, applicantPhone, epicNumber: for visitor resolution
     * - agendaType, agendaBrief, requestedLocation: appointment details
     * - schemeType, projectName, projectCategory, etc.: scheme details
     * - documents_*: file uploads for each document type
     * - associates: JSON string array of associate objects
     * - aiSummary, aiPriorityLevel: AI-generated fields
     */
    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<Map<String, Object>> submitAppointment(
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) String applicantName,
            @RequestParam(required = false) String applicantPhone,
            @RequestParam(required = false) String epicNumber,
            @RequestParam(required = false) String agendaType,
            @RequestParam(required = false) String agendaBrief,
            @RequestParam(required = false) String requestedLocation,
            @RequestParam(required = false) Boolean mlaMdcApproved,
            @RequestParam(required = false) String applicationType,
            @RequestParam(required = false) String schemeType,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String projectCategory,
            @RequestParam(required = false) String beneficiaryType,
            @RequestParam(required = false) String beneficiaryCount,
            @RequestParam(required = false) String estimatedCost,
            @RequestParam(required = false) String communityContribution,
            @RequestParam(required = false) String justification,
            @RequestParam(required = false) String organizationSubType,
            @RequestParam(required = false) String schemeHistoryList,
            @RequestParam(required = false) String associates,
            @RequestParam(required = false) String aiSummary,
            @RequestParam(required = false) String aiPriorityLevel,
            @RequestParam(required = false) Boolean consentAccepted,
            @RequestParam(required = false) String consentVersion,
            @RequestParam(required = false) String consentTimestamp,
            @RequestParam(required = false) String privacyPolicyUrl,
            @RequestParam(required = false) String termsUrl,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails user) {

        try {
            if (!hasStaffAuthority(user)) {
                Long visitorId = visitorIdFromPrincipal(user);
                if (visitorId == null) {
                    return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "message", "Authenticated visitor context is required."));
                }
                if (applicantId != null && !visitorId.equals(applicantId)) {
                    return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "message", "Applicant does not match authenticated visitor."));
                }
                validateConsent(consentAccepted, consentVersion, consentTimestamp);
                applicantId = visitorId;
            }

            // ── Resolve applicant ───────────────────────────────────────────────
            Visitor applicant = null;

            if (applicantId != null && applicantId > 0) {
                applicant = visitorRepository.findById(applicantId).orElse(null);
            }

            // If no registered visitor found, create/reuse one from form data
            if (applicant == null) {
                final String applicantNameValue = validationService.requireText(applicantName, "applicantName");
                final String applicantPhoneValue = validationService.requirePhone(applicantPhone);

                String epicFinal = epicNumber;
                if (epicFinal != null) {
                    epicFinal = epicFinal.trim().toUpperCase();
                }
                // Validate EPIC format if provided
                if (epicFinal != null && !epicFinal.isEmpty() && !epicFinal.matches(ValidationConstants.REGEX_EPIC)) {
                    epicFinal = null; // Ignore invalid EPIC rather than rejecting
                }

                List<Visitor> existingVisitors = epicFinal != null
                        ? visitorRepository.findByPhoneNumberAndEpicNumber(applicantPhoneValue, epicFinal)
                        : visitorRepository.findByPhoneNumber(applicantPhoneValue);

                if (!existingVisitors.isEmpty()) {
                    applicant = existingVisitors.get(0);
                } else {
                    // Check EPIC uniqueness if provided
                    if (epicFinal != null && visitorRepository.findByEpicNumber(epicFinal).isPresent()) {
                        applicant = visitorRepository.findByEpicNumber(epicFinal).get();
                    } else {
                        Visitor v = Visitor.builder()
                                .fullName(applicantNameValue)
                                .phoneNumber(applicantPhoneValue)
                                .epicNumber(epicFinal)
                                .kycType("NONE")
                                .kycVerified(false)
                                .kycStatus("PENDING")
                                .build();
                        applicant = visitorRepository.save(v);
                    }
                }
            }

            // ── Validate agenda ─────────────────────────────────────────────────
            final String agendaTypeValue = validationService.requireText(agendaType, "agendaType");

            // ── Resolve requestedLocation enum ──────────────────────────────────
            Appointment.MeetingLocation location;
            try {
                location = requestedLocation != null
                        ? Appointment.MeetingLocation.valueOf(requestedLocation.toUpperCase())
                        : Appointment.MeetingLocation.OTHERS;
            } catch (IllegalArgumentException e) {
                location = Appointment.MeetingLocation.OTHERS;
            }

            // ── Generate application ID ─────────────────────────────────────────
            String appId = "MC-" + DateTimeUtil.nowIST().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // ── Build and save Appointment ──────────────────────────────────────
            int meetingCount = appointmentRepository.countMeetingsLast6Months(
                    applicant.getId(), DateTimeUtil.nowIST().minusMonths(6));

            Appointment appt = Appointment.builder()
                    .applicationId(appId)
                    .applicant(applicant)
                    .eventType(Appointment.EventType.A1)
                    .agendaType(agendaTypeValue)
                    .agendaBrief(agendaBrief)
                    .appointmentCategory(Appointment.AppointmentCategory.SCHEDULED)
                    .status(Appointment.AppointmentStatus.PENDING)
                    .requestedLocation(location)
                    .mlaMdcApproved(mlaMdcApproved != null && mlaMdcApproved)
                    .consentAccepted(consentAccepted)
                    .consentVersion(trimToNull(consentVersion))
                    .consentTimestamp(parseConsentTimestamp(consentTimestamp))
                    .privacyPolicyUrl(trimToNull(privacyPolicyUrl))
                    .termsUrl(trimToNull(termsUrl))
                    .isWalkIn(false)
                    .meetingCountLast6Months(meetingCount)
                    .build();

            // Persist AI-generated fields if included in the submission
            if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                appt.setAiSummary(aiSummary.trim());
            }
            if (aiPriorityLevel != null && !aiPriorityLevel.trim().isEmpty()) {
                appt.setAiPriorityLevel(aiPriorityLevel.trim());
            }

            Appointment saved = appointmentRepository.save(appt);

            // ── Save uploaded documents ──────────────────────────────────────────
            try {
                java.util.Collection<Part> parts = request.getParts();
                for (Part part : parts) {
                    String paramName = part.getName();
                    if (paramName.startsWith("documents_")) {
                        String documentType = paramName.replace("documents_", "");
                        
                        try {
                            // Store file using FileStorageService
                            MultipartFile mfile = convertPartToMultipartFile(part);
                            if (mfile != null && !mfile.isEmpty()) {
                                FileStorageService.StoredFileMetadata storedFile =
                                        fileStorageService.storeFileSecure(mfile, applicant.getId(), appId);
                                LocalDateTime uploadedAt = DateTimeUtil.nowIST();

                                // Create and save DocumentUpload record
                                DocumentUpload docUpload = DocumentUpload.builder()
                                        .appointment(saved)
                                        .visitor(applicant)
                                        .documentType(documentType)
                                        .originalFilename(storedFile.getOriginalFileName())
                                        .storedFileName(storedFile.getStoredFileName())
                                        .filePath(storedFile.getEncryptedFilePath())
                                        .encryptedFilePath(storedFile.getEncryptedFilePath())
                                        .secureHash(storedFile.getSecureHash())
                                        .fileSizeBytes(storedFile.getFileSize())
                                        .mimeType(storedFile.getContentType())
                                        .contentType(storedFile.getContentType())
                                        .uploadedBy("visitor_" + applicant.getId())
                                        .uploadedDate(uploadedAt)
                                        .createdAt(uploadedAt)
                                        .updatedAt(uploadedAt)
                                        .build();

                                DocumentUpload savedDocument = documentUploadRepository.save(docUpload);
                                queueAiNotes(savedDocument, "visitor_" + applicant.getId());
                            }
                        } catch (IOException e) {
                            auditLogService.log("DocumentUpload", saved.getId(), "UPLOAD_ERROR",
                                    "Failed to upload document: " + documentType + " - " + e.getMessage(),
                                    "visitor_" + applicant.getId());
                            // Log error but continue processing other documents
                        }
                    }
                }
            } catch (ServletException e) {
                auditLogService.log("Appointment", saved.getId(), "PARTS_ERROR",
                        "Error retrieving multipart request parts: " + e.getMessage(),
                        "visitor_" + applicant.getId());
            }

            // ── Parse and save associates ────────────────────────────────────────
            if (associates != null && !associates.trim().isEmpty() && !associates.equals("[]")) {
                try {
                    List<Map<String, String>> associateList = objectMapper.readValue(
                            associates,
                            new TypeReference<List<Map<String, String>>>() {}
                    );

                    for (Map<String, String> assocData : associateList) {
                        AssociateMapping assoc = AssociateMapping.builder()
                                .appointment(saved)
                                .associateName(assocData.get("name"))
                                .associatePhone(assocData.get("phoneNumber"))
                                .associateEpic(assocData.get("epicNumber"))
                                .associateDesignation(assocData.get("designation"))
                                .associateAddress(assocData.get("address"))
                                .build();
                        // Note: Assuming there's a repository or service to save AssociateMapping
                        // This should be persisted through the Appointment relationship
                    }
                } catch (Exception e) {
                    auditLogService.log("App Associates", saved.getId(), "PARSE_ERROR",
                            "Failed to parse associates JSON: " + e.getMessage(),
                            "visitor_" + applicant.getId());
                    // Log error but continue - associates are optional
                }
            }

            auditLogService.log("Appointment", saved.getId(), "SUBMITTED_BY_VISITOR",
                    "Visitor appointment submitted: " + appId, "visitor_" + applicant.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("applicationId", appId);
            response.put("status", "SUBMITTED");
            response.put("message", "Appointment request submitted successfully.");
            return ResponseEntity.ok(response);

        } catch (MeghaConnectException e) {
            throw e;
        } catch (Exception e) {
            return badRequest("Error processing appointment submission: " + e.getMessage());
        }
    }

    private void queueAiNotes(DocumentUpload document, String actor) {
        try {
            appointmentDocumentAiNotesService.queueGeneration(document);
        } catch (Exception e) {
            log.warn("Unable to queue AI notes requestId={} appointmentId={} documentId={}",
                    RequestContextUtil.getRequestId(),
                    document.getAppointment() != null ? document.getAppointment().getId() : null,
                    document.getId(),
                    e);
            auditLogService.log("DocumentUpload", document.getId(), "AI_NOTES_QUEUE_FAILED",
                    "AI notes queue failed: " + e.getMessage(), actor);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Convert servlet Part to Spring MultipartFile
     */
    private MultipartFile convertPartToMultipartFile(Part part) throws IOException {
        byte[] fileContent = new byte[(int) part.getSize()];
        part.getInputStream().read(fileContent);
        
        return new MultipartFile() {
            @Override
            public String getName() {
                return part.getName();
            }

            @Override
            public String getOriginalFilename() {
                return part.getSubmittedFileName();
            }

            @Override
            public String getContentType() {
                return part.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return fileContent.length == 0;
            }

            @Override
            public long getSize() {
                return fileContent.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return fileContent;
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.ByteArrayInputStream(fileContent);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest.toPath(), fileContent);
            }

            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest, fileContent);
            }
        };
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }

    private Long visitorIdFromPrincipal(UserDetails user) {
        if (user == null || user.getUsername() == null || !user.getUsername().startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(user.getUsername().substring("visitor_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasStaffAuthority(UserDetails user) {
        return user != null && user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN")
                        || authority.equals("ROLE_APPROVER")
                        || authority.equals("ROLE_HCM")
                        || authority.equals("ROLE_DEO"));
    }

    private void validateConsent(Boolean consentAccepted, String consentVersion, String consentTimestamp) {
        if (!Boolean.TRUE.equals(consentAccepted)
                || trimToNull(consentVersion) == null
                || trimToNull(consentTimestamp) == null) {
            throw new MeghaConnectException(
                    "MISSING_CONSENT",
                    "Privacy consent is required before submitting appointment data.",
                    400
            );
        }
    }

    private LocalDateTime parseConsentTimestamp(String value) {
        String timestamp = trimToNull(value);
        if (timestamp == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (DateTimeParseException e) {
                throw new MeghaConnectException("INVALID_CONSENT_TIMESTAMP", "Consent timestamp is invalid.", 400);
            }
        }
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
