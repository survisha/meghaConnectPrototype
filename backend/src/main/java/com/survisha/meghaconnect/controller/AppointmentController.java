package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentMultipartRequest;
import com.survisha.meghaconnect.dto.AppointmentDocumentAiNotesDto;
import com.survisha.meghaconnect.dto.AppointmentDocumentDto;
import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.dto.HcmActionDto;
import com.survisha.meghaconnect.dto.ScheduleEventAppointmentAssignmentRequest;
import com.survisha.meghaconnect.dto.ScheduleEventDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.service.AppointmentDocumentAiNotesService;
import com.survisha.meghaconnect.service.AppointmentService;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.service.HcmActionService;
import com.survisha.meghaconnect.service.ScheduleEventService;
import com.survisha.meghaconnect.service.VisitorPassService;
import com.survisha.meghaconnect.service.WalkInTokenService;
import com.survisha.meghaconnect.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management and scheduling")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    private final ScheduleEventService scheduleEventService;
    private final HcmActionService hcmActionService;
    private final VisitorPassService visitorPassService;
    private final AuditLogService auditLogService;
    private final WalkInTokenService walkInTokenService;

    @Operation(summary = "Get all appointments", description = "Retrieve paginated list of all appointments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointments",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEO','HCM','SUPER_ADMIN','DEPARTMENT_PA')")
    public ResponseEntity<Page<AppointmentDto>> getAll(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String source,
                                                       @RequestParam(required = false) String appointmentType,
                                                       @RequestParam(required = false) String referredOffice,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate walkInDate,
                                                       Authentication authentication,
                                                       Pageable pageable) {
        logEndpoint("/api/v1/appointments");
        return ResponseEntity.ok(appointmentService.findAllDtosForActor(actor(authentication), status, source, appointmentType, referredOffice, walkInDate, pageable));
    }

    @GetMapping("/dashboard/walk-ins")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEO','HCM','SUPER_ADMIN')")
    public Map<String, Object> walkInDashboardCounts(Authentication authentication) {
        return Map.<String, Object>of("walkInPending", appointmentService.countWalkIns(Appointment.AppointmentStatus.PENDING, actor(authentication)),
            "walkInCompleted", appointmentService.countWalkIns(Appointment.AppointmentStatus.COMPLETED, actor(authentication)));
    }

    @GetMapping("/walk-in-dates")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEO','HCM','SUPER_ADMIN')")
    public ResponseEntity<List<LocalDate>> walkInDates() {
        logEndpoint("/api/v1/appointments/walk-in-dates");
        return ResponseEntity.ok(walkInTokenService.availableTokenDates());
    }

    @Operation(summary = "Get appointment by ID", description = "Retrieve a specific appointment by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEO','HCM','SUPER_ADMIN','DEPARTMENT_PA')")
    public ResponseEntity<AppointmentDto> getById(@PathVariable Long id, Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}");
        return appointmentService.findByIdForActor(id, actor(authentication))
            .map(appointmentService::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get appointment documents", description = "Retrieve documents attached to an appointment")
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','ADMIN','DEO')")
    public ResponseEntity<List<AppointmentDocumentDto>> getDocuments(@PathVariable Long id) {
        logEndpoint("/api/v1/appointments/{id}/documents");
        return ResponseEntity.ok(appointmentService.findDocumentDtos(id));
    }

    @Operation(summary = "Get AI notes for appointment documents", description = "Retrieve officer-facing AI notes generated from uploaded appointment documents")
    @GetMapping("/{id}/ai-notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public ResponseEntity<List<AppointmentDocumentAiNotesDto>> getAiNotes(@PathVariable Long id) {
        logEndpoint("/api/v1/appointments/{id}/ai-notes");
        return ResponseEntity.ok(appointmentDocumentAiNotesService.getNotesForAppointment(id));
    }

    @Operation(summary = "Regenerate AI notes for a document", description = "Queue AI note regeneration for an uploaded appointment document")
    @PostMapping("/documents/{documentId}/ai-notes/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public ResponseEntity<AppointmentDocumentAiNotesDto> regenerateAiNotes(@PathVariable Long documentId) {
        logEndpoint("/api/v1/appointments/documents/{documentId}/ai-notes/regenerate");
        return ResponseEntity.ok(appointmentDocumentAiNotesService.regenerate(documentId));
    }

    @Operation(summary = "Get appointment by application ID", description = "Retrieve a specific appointment by its application ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-app-id/{appId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<AppointmentDto> getByApplicationId(@PathVariable String appId) {
        logEndpoint("/api/v1/appointments/by-app-id/{appId}");
        return appointmentService.findByApplicationId(appId)
            .map(appointmentService::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get logged-in citizen appointments", description = "Retrieve appointments for the authenticated visitor")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ResponseEntity<List<AppointmentDto>> getMyAppointments(@AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/my");
        Long visitorId = visitorIdFromPrincipal(user);
        if (visitorId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(appointmentService.findMyAppointments(visitorId));
    }

    @Operation(summary = "Get visitor pass details", description = "Retrieve visitor pass metadata for a scheduled citizen appointment")
    @GetMapping("/{id}/visitor-pass")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM','SECURITY')")
    public ResponseEntity<Map<String, Object>> getVisitorPass(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/visitor-pass");
        return ResponseEntity.ok(visitorPassService.getPassDetails(id, visitorIdFromPrincipal(user)));
    }

    @Operation(summary = "Download visitor pass", description = "Download the visitor pass file for a scheduled citizen appointment")
    @GetMapping(value = "/{id}/visitor-pass/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM','SECURITY')")
    public ResponseEntity<byte[]> downloadVisitorPass(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/visitor-pass/download");
        Long visitorId = visitorIdFromPrincipal(user);
        byte[] pdf = visitorPassService.generatePassPdf(id, visitorId, user != null ? user.getUsername() : "visitor");
        String applicationId = appointmentService.findById(id)
            .map(Appointment::getApplicationId)
            .orElse(String.valueOf(id));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=\"visitor-pass-" + applicationId + ".pdf\"")
            .body(pdf);
    }

    @Operation(summary = "Get appointments for DEO", description = "Retrieve appointments visible to DEO review queues")
    @GetMapping("/deo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','APPROVER','DEO','DEPARTMENT_PA','HCM')")
    public ResponseEntity<Page<AppointmentDto>> getForDeo(Pageable pageable, Authentication authentication) {
        logEndpoint("/api/v1/appointments/deo");
        return ResponseEntity.ok(appointmentService.findForDeo(actor(authentication), pageable));
    }

    @Operation(summary = "Get appointments for approver", description = "Retrieve appointments visible to approver review queues")
    @GetMapping("/approver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','APPROVER','DEPARTMENT_PA')")
    public ResponseEntity<Page<AppointmentDto>> getForApprover(Pageable pageable, Authentication authentication) {
        logEndpoint("/api/v1/appointments/approver");
        return ResponseEntity.ok(appointmentService.findForApprover(actor(authentication), pageable));
    }

    @Operation(summary = "Assign follow-up appointments to a schedule event", description = "Link selected follow-up applications to an existing calendar event")
    @PostMapping("/assign-event")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','ADMIN','HCM')")
    public ResponseEntity<ScheduleEventDto> assignEvent(@RequestBody Map<String, Object> body,
                                                        Authentication authentication) {
        logEndpoint("/api/v1/appointments/assign-event");
        Map<String, Object> safeBody = body != null ? body : Map.of();
        Long eventId = longValue(safeBody.get("eventId"));
        ScheduleEventAppointmentAssignmentRequest request = new ScheduleEventAppointmentAssignmentRequest();
        request.setAppointmentIds(longList(safeBody.get("appointmentIds")));
        request.setRemarks(stringValue(safeBody.get("remarks")));
        return ResponseEntity.ok(scheduleEventService.assignAppointments(eventId, request, actor(authentication), role(authentication)));
    }

    @Operation(summary = "Get HCM/APPROVER action appointments for date", description = "Retrieve scheduled appointments for HCM/APPROVER action on a selected date")
    @GetMapping("/hcm-actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','APPROVER','ADMIN')")
    public ResponseEntity<List<AppointmentDto>> getHcmActionAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logEndpoint("/api/v1/appointments/hcm-actions");
        return ResponseEntity.ok(hcmActionService.getAppointmentsForDate(date)
            .stream()
            .map(appointmentService::toDto)
            .toList());
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public ResponseEntity<AppointmentDto> rescheduleAppointment(@PathVariable Long id,
                                                                @RequestBody Map<String, Object> body,
                                                                @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/reschedule");
        String actor = user != null ? user.getUsername() : "system";
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.reschedule(id, body, actor)));
    }

    @GetMapping("/{id}/remarks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','APPROVER','ADMIN','DEO','SECURITY')")
    public ResponseEntity<List<HcmActionDto>> getRemarks(@PathVariable Long id) {
        logEndpoint("/api/v1/appointments/{id}/remarks");
        return ResponseEntity.ok(hcmActionService.getRemarksForAppointment(id));
    }

    @PostMapping("/{id}/remarks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','APPROVER','ADMIN','APPROVER_JT_SECY')")
    public ResponseEntity<HcmActionDto> addRemark(@PathVariable Long id,
                                                  @RequestBody HcmActionDto body,
                                                  Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}/remarks");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(hcmActionService.addRemark(id, body, actor(authentication), role(authentication)));
    }

    @PutMapping("/{id}/remarks/{remarkId}")
    @PreAuthorize("hasAnyRole('APPROVER','APPROVER_JT_SECY','HCM')")
    public ResponseEntity<HcmActionDto> updateRemark(@PathVariable Long id,
                                                     @PathVariable Long remarkId,
                                                     @RequestBody HcmActionDto body,
                                                     Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}/remarks/{remarkId}");
        return ResponseEntity.ok(hcmActionService.updateRemark(id, remarkId, body, actor(authentication), role(authentication)));
    }

    @PostMapping("/export-audit")
    @PreAuthorize("hasAnyRole('APPROVER','APPROVER_JT_SECY','HCM')")
    public ResponseEntity<Void> auditPdfExport(@RequestBody Map<String, Object> body,
                                               Authentication authentication) {
        logEndpoint("/api/v1/appointments/export-audit");
        List<Long> appointmentIds = longList(body != null ? body.get("appointmentIds") : null);
        Object selectedCount = body != null ? body.get("selectedCount") : appointmentIds.size();
        Object filters = body != null ? body.get("filters") : null;
        auditLogService.log("Appointment", null, "PDF_EXPORT",
            "Appointment Agenda List PDF exported. selectedCount=" + selectedCount
                + ", filters=" + filters
                + ", appointmentIds=" + appointmentIds,
            actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/supporting-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','ADMIN','DEO','HCM','PUBLIC')")
    public ResponseEntity<AppointmentDocumentDto> uploadSupportingDocument(@PathVariable Long id,
                                                                           @RequestParam("file") MultipartFile file,
                                                                           @RequestParam(defaultValue = "SUPPORTING_DOCUMENT") String documentType,
                                                                           @RequestParam(required = false) String remarks,
                                                                           Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}/supporting-documents");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(appointmentService.uploadSupportingDocument(id, file, documentType, remarks,
                actor(authentication), role(authentication)));
    }

    @PostMapping("/{id}/request-missing-information")
    @PreAuthorize("hasAnyRole('APPROVER','HCM')")
    public ResponseEntity<AppointmentDto> requestMissingInformation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String remarks = body != null ? body.get("remarks") : null;
        return ResponseEntity.ok(appointmentService.toDto(
            appointmentService.requestMissingInformation(id, remarks,
                actor(authentication), role(authentication))));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public ResponseEntity<AppointmentDto> closeAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String remarks = body != null ? body.get("remarks") : null;
        return ResponseEntity.ok(appointmentService.toDto(
            appointmentService.close(id, remarks, actor(authentication), role(authentication))));
    }

    @Operation(summary = "Create appointment", description = "Create a new appointment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<Map<String, Object>> createMultipart(
            @ModelAttribute AppointmentMultipartRequest form,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments");
        String actor = user != null ? user.getUsername() : "anonymous";
        assertCanSubmitForVisitor(form != null ? form.getApplicantId() : null, user);
        Map<String, Object> response = appointmentService.createMultipart(form, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<AppointmentDto> create(@Valid @RequestBody AppointmentDto dto,
                                                 @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments");
        String actor = user != null ? user.getUsername() : "anonymous";
        assertCanSubmitForVisitor(dto != null ? dto.getApplicantId() : null, user);
        Appointment appointment = appointmentService.create(dto, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.toDto(appointment));
    }

    @Operation(summary = "Update appointment status", description = "Update the status of an appointment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment status updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "403", description = "Access denied - required role not present"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','ADMIN')")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}/status");
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.updateStatus(id, body, actor(authentication), role(authentication))));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','ADMIN')")
    public ResponseEntity<AppointmentDto> putStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        logEndpoint("/api/v1/appointments/{id}/status");
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.updateStatus(id, body, actor(authentication), role(authentication))));
    }

    @Operation(summary = "Submit CMO review", description = "Persist CMO category/location review, missing information note, and forwarding status")
    @PostMapping("/{id}/cmo-review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','ADMIN')")
    public ResponseEntity<AppointmentDto> submitCmoReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/cmo-review");
        String actor = user != null ? user.getUsername() : "system";
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.submitCmoReview(id, body, actor)));
    }

    @Operation(summary = "Schedule appointment", description = "Schedule an appointment with date and duration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment scheduled successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied - required role not present"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','ADMIN')")
    public ResponseEntity<AppointmentDto> schedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/schedule");
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.schedule(id, body, user.getUsername())));
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

    private void assertCanSubmitForVisitor(Long applicantId, UserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        if (hasStaffAuthority(user)) {
            return;
        }
        Long visitorId = visitorIdFromPrincipal(user);
        if (visitorId != null && visitorId.equals(applicantId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Applicant does not match authenticated visitor.");
    }

    private boolean hasStaffAuthority(UserDetails user) {
        return user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals("ROLE_ADMIN")
                || authority.equals("ROLE_APPROVER")
                || authority.equals("ROLE_HCM")
                || authority.equals("ROLE_SUPER_ADMIN")
                || authority.equals("ROLE_DEPARTMENT_ADMIN")
                || authority.equals("ROLE_DEPARTMENT_PA")
                || authority.equals("ROLE_DEO"));
    }

    private void logEndpoint(String endpoint) {
        log.info("Appointment API request requestId={} endpoint={}", RequestContextUtil.getRequestId(), endpoint);
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
            .map(this::longValue)
            .filter(item -> item != null && item > 0)
            .distinct()
            .toList();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String role(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "ANONYMOUS";
        }
        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .map(authority -> authority.replace("ROLE_", ""))
            .orElse("ANONYMOUS");
    }
}
