package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.HcmActionDto;
import com.survisha.meghaconnect.service.HcmActionService;
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
public class HcmActionController {
    
    private final HcmActionService hcmActionService;
    
    /**
     * Get all pending work items for HCM dashboard
     */
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
    @GetMapping("/{actionId}")
    public ResponseEntity<?> getActionDetails(@PathVariable Long actionId, HttpServletRequest request) {
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
    @GetMapping("/recent/{days}")
    public ResponseEntity<?> getRecentPendingActions(@PathVariable int days, HttpServletRequest request) {
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
