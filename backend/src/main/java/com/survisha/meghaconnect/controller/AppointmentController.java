package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentMultipartRequest;
import com.survisha.meghaconnect.dto.AppointmentDocumentDto;
import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.service.AppointmentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Appointments", description = "Appointment management and scheduling")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Get all appointments", description = "Retrieve paginated list of all appointments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointments",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<AppointmentDto>> getAll(Pageable pageable) {
        logEndpoint("/api/v1/appointments");
        return ResponseEntity.ok(appointmentService.findAllDtos(pageable));
    }

    @Operation(summary = "Get appointment by ID", description = "Retrieve a specific appointment by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getById(@PathVariable Long id) {
        logEndpoint("/api/v1/appointments/{id}");
        return appointmentService.findById(id)
            .map(appointmentService::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get appointment documents", description = "Retrieve documents attached to an appointment")
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<List<AppointmentDocumentDto>> getDocuments(@PathVariable Long id) {
        logEndpoint("/api/v1/appointments/{id}/documents");
        return ResponseEntity.ok(appointmentService.findDocumentDtos(id));
    }

    @Operation(summary = "Get appointment by application ID", description = "Retrieve a specific appointment by its application ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-app-id/{appId}")
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

    @Operation(summary = "Get appointments for DEO", description = "Retrieve appointments visible to DEO review queues")
    @GetMapping("/deo")
    @PreAuthorize("hasAnyRole('ADMIN','OSD','DATA_ENTRY_OPERATOR','CMO_OFFICER')")
    public ResponseEntity<Page<AppointmentDto>> getForDeo(Pageable pageable) {
        logEndpoint("/api/v1/appointments/deo");
        return ResponseEntity.ok(appointmentService.findForDeo(pageable));
    }

    @Operation(summary = "Get appointments for approver", description = "Retrieve appointments visible to approver review queues")
    @GetMapping("/approver")
    @PreAuthorize("hasAnyRole('HCM','ADMIN','OSD','APPROVER','CMO_OFFICER')")
    public ResponseEntity<Page<AppointmentDto>> getForApprover(Pageable pageable) {
        logEndpoint("/api/v1/appointments/approver");
        return ResponseEntity.ok(appointmentService.findForApprover(pageable));
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
    public ResponseEntity<Map<String, Object>> createMultipart(
            @ModelAttribute AppointmentMultipartRequest form,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments");
        String actor = user != null ? user.getUsername() : "anonymous";
        Map<String, Object> response = appointmentService.createMultipart(form, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AppointmentDto> create(@Valid @RequestBody AppointmentDto dto,
                                                 @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments");
        String actor = user != null ? user.getUsername() : "anonymous";
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
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/status");
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.updateStatus(id, body, user.getUsername())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ResponseEntity<AppointmentDto> putStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/appointments/{id}/status");
        return ResponseEntity.ok(appointmentService.toDto(appointmentService.updateStatus(id, body, user.getUsername())));
    }

    @Operation(summary = "Submit CMO review", description = "Persist CMO category/location review, missing information note, and forwarding status")
    @PostMapping("/{id}/cmo-review")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','OSD','ADMIN')")
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
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
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

    private void logEndpoint(String endpoint) {
        log.info("Appointment API request requestId={} endpoint={}", RequestContextUtil.getRequestId(), endpoint);
    }
}
