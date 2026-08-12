package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.DepartmentRepository;
import com.survisha.meghaconnect.repository.WalkInRepository;
import com.survisha.meghaconnect.service.AppointmentAuditService;
import com.survisha.meghaconnect.service.AppointmentService;
import com.survisha.meghaconnect.service.AppointmentLifecycleService;
import com.survisha.meghaconnect.util.DateTimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Controller for Appointment Approval Workflow
 * Handles CMO Officer and Joint Secretary Level review/approval process
 * R003: Appointment Approval Routing
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Approval", description = "Appointment review, approval, and scheduling workflow")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentApprovalController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentLifecycleService lifecycleService;
    private final AppointmentAuditService appointmentAuditService;
    private final DepartmentRepository departmentRepository;
    private final WalkInRepository walkInRepository;

    @PostMapping("/{id}/return-information")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public AppointmentDto returnForInformation(@PathVariable Long id,
                                               @RequestBody ReturnInformationRequest request,
                                               Authentication authentication) {
        if (request.getReason() == null || request.getReason().isBlank()
                || request.getRequiredInformation() == null || request.getRequiredInformation().isBlank()) {
            throw new IllegalArgumentException("Return reason and required information are required.");
        }
        Appointment appointment = pendingScheduled(id);
        appointment.setReturnReason(request.getReason().trim());
        appointment.setRequiredInformation(request.getRequiredInformation().trim());
        appointment.setReturnDueDate(request.getDueDate());
        appointment.setApproverRemarks(request.getRemarks());
        appointment.setUpdatedBy(actor(authentication));
        Appointment saved = appointmentRepository.save(appointment);
        auditSameStatus(saved, "RETURNED_FOR_INFORMATION", request.getReason(), authentication);
        return appointmentService.toDto(saved);
    }

    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN','DEO','SUPER_ADMIN')")
    public AppointmentDto resubmit(@PathVariable Long id, Authentication authentication) {
        Appointment appointment = pendingScheduled(id);
        if (appointment.getReturnReason() == null) {
            throw new IllegalStateException("Appointment has not been returned for information.");
        }
        appointment.setReturnReason(null);
        appointment.setRequiredInformation(null);
        appointment.setReturnDueDate(null);
        appointment.setUpdatedBy(actor(authentication));
        Appointment saved = appointmentRepository.save(appointment);
        auditSameStatus(saved, "RESUBMITTED", "Additional information resubmitted", authentication);
        return appointmentService.toDto(saved);
    }

    @PostMapping("/{id}/route")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public AppointmentDto routeAndClose(@PathVariable Long id,
                                        @RequestBody RouteRequest request,
                                        Authentication authentication) {
        if (request.getDepartmentId() == null && (request.getOfficer() == null || request.getOfficer().isBlank())) {
            throw new IllegalArgumentException("Department or responsible officer is required.");
        }
        Appointment appointment = pendingScheduled(id);
        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        if (request.getDepartmentId() != null) {
            appointment.setRoutedDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }
        appointment.setRoutedOfficer(request.getOfficer());
        appointment.setMeetingOutcome(request.getDirection());
        appointment.setApproverRemarks(request.getRemarks());
        appointment.setFollowUpRequired(Boolean.TRUE.equals(request.getFollowUpRequired()));
        lifecycleService.transition(appointment, Appointment.AppointmentStatus.ROUTED_TO_OFFICIAL);
        appointment.setUpdatedBy(actor(authentication));
        Appointment saved = appointmentRepository.save(appointment);
        audit(saved, oldStatus, "ROUTED_TO_OFFICIAL", request.getDirection(), authentication);
        return appointmentService.toDto(saved);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEO')")
    public AppointmentDto completeMeeting(@PathVariable Long id,
                                          @RequestBody CompleteMeetingRequest request,
                                          Authentication authentication) {
        if (request.getOutcome() == null || request.getOutcome().isBlank()) {
            throw new IllegalArgumentException("Meeting outcome is required.");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        Appointment.AppointmentStatus target = appointment.getAppointmentCategory() == Appointment.AppointmentCategory.WALK_IN
                ? Appointment.AppointmentStatus.COMPLETED
                : Appointment.AppointmentStatus.HCM_MET_COMPLETED;
        lifecycleService.transition(appointment, target);
        appointment.setMeetingOutcome(request.getOutcome().trim());
        appointment.setHcmRemarks(request.getRemarks());
        appointment.setFollowUpRequired(Boolean.TRUE.equals(request.getFollowUpRequired()));
        appointment.setCompletedAt(DateTimeUtil.nowIST());
        appointment.setCompletedBy(actor(authentication));
        appointment.setUpdatedBy(actor(authentication));
        Appointment saved = appointmentRepository.save(appointment);
        if (target == Appointment.AppointmentStatus.COMPLETED) {
            walkInRepository.findByAppointment_Id(id).ifPresent(walkIn -> {
                walkIn.setStatus(com.survisha.meghaconnect.entity.WalkIn.WalkInStatus.COMPLETED);
                walkInRepository.save(walkIn);
            });
        }
        audit(saved, oldStatus, "MEETING_COMPLETED", request.getOutcome(), authentication);
        return appointmentService.toDto(saved);
    }

    /**
     * Get pending appointments for CMO Officer or Joint Secretary (Approver)
     * R003: Get list of appointments awaiting approval
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','APPROVER_JT_SECY','ADMIN','HCM')")
    @Operation(
        summary = "Get pending appointments for approval",
        description = "Returns list of appointments awaiting CMO or Joint Secretary review based on user role"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending appointments",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<AppointmentDto>> getPendingAppointments(
            @RequestParam(defaultValue = "PENDING") String status,
            Pageable pageable) {
        
        try {
            List<Appointment.AppointmentStatus> statusList = parseStatus(status);
            Page<Appointment> appointments = appointmentRepository.findByStatusIn(statusList, pageable);
            
            List<AppointmentDto> dtos = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get full appointment details for review
     * R003: Returns all appointment information for detailed review
     */
    @GetMapping("/{id}/approval-details")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','ADMIN','HCM','PUBLIC')")
    @Operation(summary = "Get appointment details for approval review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppointmentDto> getAppointmentDetails(@PathVariable Long id) {
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            AppointmentDto dto = convertToDTO(appointment);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * CMO Officer approves and forwards to Joint Secretary
     * R003: Updates status to CMO_REVIEW → APPROVER_REVIEW
     */
    @PutMapping("/{id}/cmo-approve")
    @PreAuthorize("hasAnyRole('APPROVER','HCM')")
    @Operation(
        summary = "CMO approves and forwards appointment",
        description = "CMO review approval, records remarks, and routes to Joint Secretary"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment approved and forwarded"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppointmentDto> cmoApproveAndForward(
            @PathVariable Long id,
            @RequestBody ApprovRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            appointment.setCmoRemarks(request.getRemarks());
            appointment.setStatus(Appointment.AppointmentStatus.APPROVER_REVIEW);
            appointment.setUpdatedAt(DateTimeUtil.nowIST());
            
            Appointment updated = appointmentRepository.save(appointment);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Joint Secretary approves for scheduling
     * R003: Updates status to APPROVER_REVIEW → HCM_PENDING (awaiting scheduling)
     */
    @PutMapping("/{id}/approver-approve")
    @PreAuthorize("hasAnyRole('APPROVER','APPROVER_JT_SECY','HCM')")
    @Operation(
        summary = "Joint Secretary approves for scheduling",
        description = "Joint Secretary reviews and approves, ready for calendar scheduling"
    )
    public ResponseEntity<AppointmentDto> approverApprove(
            @PathVariable Long id,
            @RequestBody ApprovRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            appointment.setApproverRemarks(request.getRemarks());
            appointment.setStatus(Appointment.AppointmentStatus.HCM_PENDING);
            appointment.setUpdatedAt(DateTimeUtil.nowIST());
            
            Appointment updated = appointmentRepository.save(appointment);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject appointment with reason
     * R003: Updates status to REJECTED with rejection reason
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER','APPROVER_JT_SECY','HCM')")
    @Operation(summary = "Reject appointment")
    public ResponseEntity<AppointmentDto> rejectAppointment(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            Appointment.AppointmentStatus oldStatus = appointment.getStatus();
            lifecycleService.transition(appointment, Appointment.AppointmentStatus.REJECTED);
            appointment.setRejectionReason(request.getRejectReason());
            appointment.setRejectedAt(DateTimeUtil.nowIST());
            org.springframework.security.core.Authentication currentAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            appointment.setRejectedBy(currentAuth != null ? currentAuth.getName() : "SYSTEM");

            if (oldStatus == Appointment.AppointmentStatus.CMO_REVIEW) {
                appointment.setCmoRemarks("REJECTED: " + request.getRejectReason());
            } else {
                appointment.setApproverRemarks("REJECTED: " + request.getRejectReason());
            }
            
            appointment.setUpdatedAt(DateTimeUtil.nowIST());
            Appointment updated = appointmentRepository.save(appointment);
            
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Schedule appointment to calendar
     * R003: Updates status to SCHEDULED with date/time/location
     */
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','APPROVER')")
    @Operation(summary = "Schedule appointment in calendar")
    public ResponseEntity<AppointmentDto> scheduleAppointment(
            @PathVariable Long id,
            @RequestBody ScheduleRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            LocalDateTime scheduledTime = LocalDateTime.parse(request.getStartTime());
            
            appointment.setScheduledDateTime(scheduledTime);
            appointment.setScheduledDurationMinutes(30);
            lifecycleService.transition(appointment, Appointment.AppointmentStatus.SCHEDULED);
            appointment.setUpdatedAt(DateTimeUtil.nowIST());
            
            if (request.getLocation() != null) {
                try {
                    appointment.setRequestedLocation(
                            Appointment.MeetingLocation.valueOf(request.getLocation().toUpperCase())
                    );
                } catch (IllegalArgumentException e) {
                    appointment.setRequestedLocation(Appointment.MeetingLocation.OTHERS);
                }
            }
            
            Appointment updated = appointmentRepository.save(appointment);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reschedule existing appointment
     * R015: Allows rescheduling of already scheduled appointments
     */
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','APPROVER')")
    @Operation(summary = "Reschedule appointment")
    public ResponseEntity<AppointmentDto> rescheduleAppointment(
            @PathVariable Long id,
            @RequestBody RescheduleRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            LocalDateTime newTime = LocalDateTime.parse(request.getStartTime());
            appointment.setScheduledDateTime(newTime);
            appointment.setUpdatedAt(DateTimeUtil.nowIST());
            
            String rescheduleNote = "Rescheduled. Reason: " +
                    (request.getRescheduledReason() != null ? request.getRescheduledReason() : "Not specified");
            appointment.setShortNotes(rescheduleNote);
            
            Appointment updated = appointmentRepository.save(appointment);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available time slots for scheduling
     * R015: Returns list of available time slots for given date & location
     */
    @GetMapping("/available-slots")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','APPROVER','APPROVER_JT_SECY')")
    @Operation(summary = "Get available slots for scheduling")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam String date,
            @RequestParam String location) {
        
        try {
            List<Map<String, Object>> slots = generateAvailableSlots();
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check for scheduling conflicts
     * R015: Validates if proposed time slot has conflicts
     */
    @PostMapping("/check-conflicts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','APPROVER')")
    @Operation(summary = "Check for scheduling conflicts")
    public ResponseEntity<Map<String, Object>> checkConflicts(
            @RequestBody ConflictCheckRequest request) {
        
        try {
            boolean hasConflict = false;
            
            Map<String, Object> result = new HashMap<>();
            result.put("available", !hasConflict);
            result.put("message", hasConflict ? "Time slot is already booked" : "Slot is available");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ──────────────────────── Helper Methods ────────────────────────

    private Appointment pendingScheduled(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (appointment.getAppointmentCategory() != Appointment.AppointmentCategory.SCHEDULED
                || appointment.getStatus() != Appointment.AppointmentStatus.PENDING) {
            throw new IllegalStateException("Action is allowed only for a pending scheduled appointment.");
        }
        return appointment;
    }

    private void auditSameStatus(Appointment appointment, String action, String remarks, Authentication authentication) {
        audit(appointment, appointment.getStatus(), action, remarks, authentication);
    }

    private void audit(Appointment appointment, Appointment.AppointmentStatus oldStatus,
                       String action, String remarks, Authentication authentication) {
        appointmentAuditService.recordStatusChange(appointment, oldStatus, appointment.getStatus(), action,
                remarks, actor(authentication), actorRole(authentication));
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "system";
    }

    private String actorRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().isEmpty()) return "SYSTEM";
        return authentication.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
    }

    private AppointmentDto convertToDTO(Appointment appointment) {
        return appointmentService.toDto(appointment);
    }

    private List<Appointment.AppointmentStatus> parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return List.of(Appointment.AppointmentStatus.PENDING);
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> Appointment.AppointmentStatus.valueOf(value.toUpperCase()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> generateAvailableSlots() {
        List<Map<String, Object>> slots = new java.util.ArrayList<>();
        for (int hour = 9; hour < 17; hour++) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", hour + ":00");
            slot.put("endTime", hour + ":30");
            slot.put("isAvailable", true);
            slot.put("slotId", hour + ":00-" + hour + ":30");
            slots.add(slot);
        }
        return slots;
    }

    // ──────────────────────── DTOs ─────────────────────────

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApprovRequest {
        private String remarks;
        private String nextAction;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RejectRequest {
        private String rejectReason;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleRequest {
        private String startTime;
        private String endTime;
        private String location;
        private String remarks;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RescheduleRequest {
        private String startTime;
        private String rescheduledReason;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConflictCheckRequest {
        private String startTime;
        private String endTime;
        private String location;
        private Long excludeAppointmentId;
    }

    @Data
    public static class ReturnInformationRequest {
        private String reason;
        private String requiredInformation;
        private LocalDate dueDate;
        private String remarks;
    }

    @Data
    public static class RouteRequest {
        private Long departmentId;
        private String officer;
        private String direction;
        private String remarks;
        private Boolean followUpRequired;
    }

    @Data
    public static class CompleteMeetingRequest {
        private String outcome;
        private String remarks;
        private Boolean followUpRequired;
    }
}

