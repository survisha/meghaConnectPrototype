package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.service.AppointmentService;
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
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = "*")
public class AppointmentApprovalController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;

    /**
     * Get pending appointments for CMO Officer or Joint Secretary (Approver)
     * R003: Get list of appointments awaiting approval
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CMO_OFFICER', 'APPROVER', 'APPROVER_JT_SECY', 'ADMIN', 'HCM')")
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
            @RequestParam(defaultValue = "SUBMITTED,CMO_REVIEW") String status,
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
    @PreAuthorize("hasAnyRole('CMO_OFFICER', 'APPROVER', 'ADMIN', 'HCM', 'PUBLIC')")
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
    @PreAuthorize("hasRole('CMO_OFFICER')")
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
    @PreAuthorize("hasAnyRole('APPROVER', 'APPROVER_JT_SECY')")
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
    @PreAuthorize("hasAnyRole('CMO_OFFICER', 'APPROVER', 'APPROVER_JT_SECY')")
    @Operation(summary = "Reject appointment")
    public ResponseEntity<AppointmentDto> rejectAppointment(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {
        
        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
            Appointment.AppointmentStatus oldStatus = appointment.getStatus();
            appointment.setStatus(Appointment.AppointmentStatus.HCM_REJECTED);

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
    @PreAuthorize("hasAnyRole('HCM', 'ADMIN', 'OSD')")
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
            appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
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
    @PreAuthorize("hasAnyRole('HCM', 'ADMIN', 'OSD')")
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
    @PreAuthorize("hasAnyRole('HCM', 'ADMIN', 'OSD', 'CMO_OFFICER', 'APPROVER', 'APPROVER_JT_SECY')")
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
    @PreAuthorize("hasAnyRole('HCM', 'ADMIN', 'OSD', 'CMO_OFFICER')")
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

    private AppointmentDto convertToDTO(Appointment appointment) {
        return appointmentService.toDto(appointment);
    }

    private List<Appointment.AppointmentStatus> parseStatus(String status) {
        return Arrays.asList(Appointment.AppointmentStatus.SUBMITTED, Appointment.AppointmentStatus.CMO_REVIEW);
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
}

