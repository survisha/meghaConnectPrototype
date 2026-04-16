package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.HcmActionDto;
import com.survisha.meghaconnect.service.HcmActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * HcmActionController - REST endpoints for HCM gesture-based actions
 * Right Swipe (Accept/Modify) and Left Swipe (Reject/Delay) gestures
 */
@RestController
@RequestMapping("/api/v1/hcm/actions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "HCM Actions", description = "HCM gesture-based appointment actions (swipe gestures, acceptance, rejection, snooping)")
@SecurityRequirement(name = "bearerAuth")
public class HcmActionController {
    
    private final HcmActionService hcmActionService;
    
    /**
     * Get all pending work items for HCM dashboard
     */
    @Operation(summary = "Get pending work items", description = "Retrieve all pending appointments for HCM with action options")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending work items",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = HcmActionDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pending-work")
    public ResponseEntity<?> getPendingWorkItems(HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            List<HcmActionDto> pendingWork = hcmActionService.getPendingWorkItems();
            return ResponseEntity.ok(pendingWork);
        } catch (Exception e) {
            log.error("Error fetching pending work items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching pending work items");
        }
    }
    
    /**
     * Get pending work count for badge
     */
    @Operation(summary = "Get pending work count", description = "Get count of pending appointments for badge display")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending work count"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pending-work/count")
    public ResponseEntity<?> getPendingWorkCount(HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            Long count = hcmActionService.getPendingWorkItemCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error fetching pending work count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching pending work count");
        }
    }
    
    /**
     * Get pending actions for specific appointment
     */
    @Operation(summary = "Get appointment actions", description = "Retrieve pending actions for a specific appointment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment actions"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getAppointmentActions(@PathVariable Long appointmentId, HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            List<HcmActionDto> actions = hcmActionService.getPendingActionsForAppointment(appointmentId);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            log.error("Error fetching appointment actions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching appointment actions");
        }
    }
    
    /**
     * Accept appointment (Right Swipe - Option 1)
     */
    @Operation(summary = "Accept appointment", description = "Accept appointment with suggested date/time (Right Swipe Option 1)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment accepted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointment/{appointmentId}/accept")
    public ResponseEntity<?> acceptAppointment(@PathVariable Long appointmentId,
                                               @RequestBody HcmActionDto actionDto,
                                               HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            HcmActionDto result = hcmActionService.acceptAppointment(appointmentId, actionDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error accepting appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error accepting appointment");
        }
    }
    
    /**
     * Mark important and reschedule earlier (Right Swipe - Option 2)
     */
    @Operation(summary = "Mark as important", description = "Mark appointment as important and reschedule for earlier date (Right Swipe Option 2)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment marked as important successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointment/{appointmentId}/mark-important")
    public ResponseEntity<?> markImportantAndReschedule(@PathVariable Long appointmentId,
                                                        @RequestBody HcmActionDto actionDto,
                                                        HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            HcmActionDto result = hcmActionService.markImportantAndReschedule(appointmentId, actionDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error marking appointment as important", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error marking appointment as important");
        }
    }
    
    /**
     * Modify appointment date/time (Right Swipe - Option 3)
     */
    @Operation(summary = "Modify appointment", description = "Change appointment date and time with remarks (Right Swipe Option 3)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment modified successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointment/{appointmentId}/modify")
    public ResponseEntity<?> modifyAppointmentDateTime(@PathVariable Long appointmentId,
                                                       @RequestBody HcmActionDto actionDto,
                                                       HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            HcmActionDto result = hcmActionService.modifyAppointmentDateTime(appointmentId, actionDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error modifying appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error modifying appointment");
        }
    }
    
    /**
     * Snooze appointment (Left Swipe - Option 1)
     */
    @Operation(summary = "Snooze appointment", description = "Defer appointment for 7, 15, 30 days or custom days (Left Swipe Option 1)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment snoozed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointment/{appointmentId}/snooze")
    public ResponseEntity<?> snoozeAppointment(@PathVariable Long appointmentId,
                                               @RequestBody HcmActionDto actionDto,
                                               HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            HcmActionDto result = hcmActionService.snoozeAppointment(appointmentId, actionDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error snoozing appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error snoozing appointment");
        }
    }
    
    /**
     * Reject and request clarification (Left Swipe - Option 2)
     */
    @Operation(summary = "Reject appointment", description = "Reject appointment and request clarification from CMO (Left Swipe Option 2)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment rejected successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointment/{appointmentId}/reject")
    public ResponseEntity<?> rejectAndRequestClarification(@PathVariable Long appointmentId,
                                                           @RequestBody HcmActionDto actionDto,
                                                           HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            HcmActionDto result = hcmActionService.rejectAndRequestClarification(appointmentId, actionDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error rejecting appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error rejecting appointment");
        }
    }
    
    /**
     * Get action details
     */
    @Operation(summary = "Get action details", description = "Retrieve detailed information about a specific HCM action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved action details"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "404", description = "Action not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{actionId}")
    public ResponseEntity<?> getActionDetails(@Parameter(description = "Action ID") @PathVariable Long actionId, HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            Optional<HcmActionDto> action = hcmActionService.getActionDetails(actionId);
            if (action.isPresent()) {
                return ResponseEntity.ok(action.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Action not found");
            }
        } catch (Exception e) {
            log.error("Error fetching action details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching action details");
        }
    }
    
    /**
     * Get recent pending actions
     */
    @Operation(summary = "Get recent actions", description = "Retrieve HCM actions from the last N days")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent actions"),
        @ApiResponse(responseCode = "403", description = "Access denied - HCM role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/recent/{days}")
    public ResponseEntity<?> getRecentPendingActions(@Parameter(description = "Number of days to look back") @PathVariable int days, HttpServletRequest request) {
        try {
            if (!hcmActionService.isHcmUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. HCM role required.");
            }
            
            List<HcmActionDto> recentActions = hcmActionService.getRecentPendingActions(days);
            return ResponseEntity.ok(recentActions);
        } catch (Exception e) {
            log.error("Error fetching recent pending actions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching recent pending actions");
        }
    }
}
