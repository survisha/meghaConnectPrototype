package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.HcmActionDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.HcmAction;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.HcmActionRepository;
import com.survisha.meghaconnect.security.JwtUtils;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HcmActionService - Handles all HCM gesture-based actions on appointments/meetings
 * Right Swipe (Accept/Modify) and Left Swipe (Reject/Delay) gestures
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HcmActionService {
    
    private final HcmActionRepository hcmActionRepository;
    private final AppointmentRepository appointmentRepository;
    private final JwtUtils jwtUtils;
    
    /**
     * Get all pending work items for HCM dashboard
     */
    public List<HcmActionDto> getPendingWorkItems() {
        log.debug("Fetching all pending work items");
        return hcmActionRepository.findAllPendingActions()
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending actions for a specific appointment
     */
    public List<HcmActionDto> getPendingActionsForAppointment(Long appointmentId) {
        log.debug("Fetching pending actions for appointment: {}", appointmentId);
        return hcmActionRepository.findActiveActionsByAppointment(appointmentId)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Accept appointment with original date/time (Right Swipe - Option 1)
     */
    public HcmActionDto acceptAppointment(Long appointmentId, HcmActionDto actionDto, HttpServletRequest request) {
        log.info("HCM accepting appointment: {}", appointmentId);
        
        LocalDateTime acceptedDateTime = actionDto.getAcceptedDateTime() != null
            ? actionDto.getAcceptedDateTime()
            : DateTimeUtil.nowIST();

        HcmAction action = HcmAction.builder()
            .appointmentId(appointmentId)
            .actionType("ACCEPT")
            .actionStatus("COMPLETED")
            .gestureType("RIGHT_SWIPE")
            .acceptedDateTime(acceptedDateTime)
            .hcmRemarks(actionDto.getHcmRemarks())
            .originalDateTime(actionDto.getOriginalDateTime())
            .originalLocation(actionDto.getOriginalLocation())
            .appointmentSubject(actionDto.getAppointmentSubject())
            .build();
        
        HcmAction saved = hcmActionRepository.save(action);
        updateAppointmentAfterHcmAcceptance(appointmentId, acceptedDateTime, actionDto.getHcmRemarks());
        log.info("Appointment accepted by HCM: appointmentId={}", appointmentId);
        return convertToDto(saved);
    }
    
    /**
     * Mark appointment as important and reschedule earlier (Right Swipe - Option 2)
     */
    public HcmActionDto markImportantAndReschedule(Long appointmentId, HcmActionDto actionDto, HttpServletRequest request) {
        log.info("HCM marking appointment as important: {}", appointmentId);
        
        HcmAction action = HcmAction.builder()
            .appointmentId(appointmentId)
            .actionType("MARK_IMPORTANT")
            .actionStatus("PENDING")
            .gestureType("RIGHT_SWIPE")
            .isImportantMeeting(true)
            .requestedEarlierDateTime(actionDto.getRequestedEarlierDateTime())
            .hcmRemarks(actionDto.getHcmRemarks())
            .originalDateTime(actionDto.getOriginalDateTime())
            .originalLocation(actionDto.getOriginalLocation())
            .appointmentSubject(actionDto.getAppointmentSubject())
            .build();
        
        HcmAction saved = hcmActionRepository.save(action);
        log.info("Appointment marked as important with earlier reschedule request: appointmentId={}", appointmentId);
        return convertToDto(saved);
    }
    
    /**
     * Modify appointment date/time with comments (Right Swipe - Option 3)
     */
    public HcmActionDto modifyAppointmentDateTime(Long appointmentId, HcmActionDto actionDto, HttpServletRequest request) {
        log.info("HCM modifying appointment date/time: {}", appointmentId);
        
        HcmAction action = HcmAction.builder()
            .appointmentId(appointmentId)
            .actionType("ACCEPT_WITH_CHANGES")
            .actionStatus("PENDING")
            .gestureType("RIGHT_SWIPE")
            .acceptedDateTime(actionDto.getAcceptedDateTime())
            .hcmRemarks(actionDto.getHcmRemarks())
            .originalDateTime(actionDto.getOriginalDateTime())
            .originalLocation(actionDto.getOriginalLocation())
            .appointmentSubject(actionDto.getAppointmentSubject())
            .build();
        
        HcmAction saved = hcmActionRepository.save(action);
        log.info("Appointment date/time modified by HCM: appointmentId={}", appointmentId);
        return convertToDto(saved);
    }
    
    /**
     * Snooze appointment request (Left Swipe - Option 1)
     */
    public HcmActionDto snoozeAppointment(Long appointmentId, HcmActionDto actionDto, HttpServletRequest request) {
        log.info("HCM snoozing appointment: {} for {}", appointmentId, actionDto.getSnoozeType());
        
        Integer durationDays = actionDto.getSnoozeDurationDays();
        if (durationDays == null) {
            if ("DAYS_7".equals(actionDto.getSnoozeType())) {
                durationDays = 7;
            } else if ("DAYS_15".equals(actionDto.getSnoozeType())) {
                durationDays = 15;
            } else if ("DAYS_30".equals(actionDto.getSnoozeType())) {
                durationDays = 30;
            } else {
                durationDays = 7;
            }
        }
        
        LocalDateTime snoozedUntil = DateTimeUtil.nowIST().plusDays(durationDays);
        
        HcmAction action = HcmAction.builder()
            .appointmentId(appointmentId)
            .actionType("SNOOZE")
            .actionStatus("PENDING")
            .gestureType("LEFT_SWIPE")
            .snoozeType(actionDto.getSnoozeType())
            .snoozeDurationDays(durationDays)
            .snoozedUntil(snoozedUntil)
            .hcmRemarks(actionDto.getHcmRemarks())
            .originalDateTime(actionDto.getOriginalDateTime())
            .originalLocation(actionDto.getOriginalLocation())
            .appointmentSubject(actionDto.getAppointmentSubject())
            .build();
        
        HcmAction saved = hcmActionRepository.save(action);
        log.info("Appointment snoozed by HCM until: appointmentId={}, snoozedUntil={}", appointmentId, snoozedUntil);
        return convertToDto(saved);
    }
    
    /**
     * Reject appointment and request clarification (Left Swipe - Option 2)
     */
    public HcmActionDto rejectAndRequestClarification(Long appointmentId, HcmActionDto actionDto, HttpServletRequest request) {
        log.info("HCM rejecting appointment and requesting clarification: {}", appointmentId);
        
        HcmAction action = HcmAction.builder()
            .appointmentId(appointmentId)
            .actionType("REJECT")
            .actionStatus("PENDING")
            .gestureType("LEFT_SWIPE")
            .isRejected(true)
            .clarificationRequested(actionDto.getClarificationRequested())
            .hcmRemarks(actionDto.getHcmRemarks())
            .originalDateTime(actionDto.getOriginalDateTime())
            .originalLocation(actionDto.getOriginalLocation())
            .appointmentSubject(actionDto.getAppointmentSubject())
            .build();
        
        HcmAction saved = hcmActionRepository.save(action);
        log.info("Appointment rejected by HCM with clarification request: appointmentId={}", appointmentId);
        return convertToDto(saved);
    }
    
    /**
     * Get action details
     */
    public Optional<HcmActionDto> getActionDetails(Long actionId) {
        log.debug("Fetching action details: {}", actionId);
        return hcmActionRepository.findById(actionId)
            .map(this::convertToDto);
    }
    
    /**
     * Get recent pending actions (for dashboard)
     */
    public List<HcmActionDto> getRecentPendingActions(int days) {
        log.debug("Fetching pending actions from last {} days", days);
        LocalDateTime since = DateTimeUtil.nowIST().minusDays(days);
        return hcmActionRepository.findRecentPendingActions(since)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Count pending work items
     */
    public Long getPendingWorkItemCount() {
        return hcmActionRepository.countPendingActions();
    }
    
    /**
     * Confirm/complete a pending action (mark as handled by CMO/Approver)
     */
    public HcmActionDto confirmAction(Long actionId) {
        log.info("Confirming HCM action: {}", actionId);
        
        HcmAction action = hcmActionRepository.findById(actionId)
            .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));
        
        action.setActionStatus("CONFIRMED");
        HcmAction saved = hcmActionRepository.save(action);
        log.info("HCM action confirmed: {}", actionId);
        return convertToDto(saved);
    }
    
    /**
     * Check if user is HCM
     */
    public boolean isHcmUser(HttpServletRequest request) {
        try {
            String token = jwtUtils.extractTokenFromRequest(request);
            if (token == null) return false;
            String role = jwtUtils.getRoleFromToken(token);
            return "HCM".equals(role);
        } catch (Exception e) {
            log.error("Error checking HCM role", e);
            return false;
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private HcmActionDto convertToDto(HcmAction action) {
        return HcmActionDto.builder()
            .id(action.getId())
            .appointmentId(action.getAppointmentId())
            .actionType(action.getActionType())
            .actionStatus(action.getActionStatus())
            .acceptedDateTime(action.getAcceptedDateTime())
            .isImportantMeeting(action.getIsImportantMeeting())
            .requestedEarlierDateTime(action.getRequestedEarlierDateTime())
            .snoozeType(action.getSnoozeType())
            .snoozeDurationDays(action.getSnoozeDurationDays())
            .snoozedUntil(action.getSnoozedUntil())
            .isRejected(action.getIsRejected())
            .clarificationRequested(action.getClarificationRequested())
            .hcmRemarks(action.getHcmRemarks())
            .gestureType(action.getGestureType())
            .originalDateTime(action.getOriginalDateTime())
            .originalLocation(action.getOriginalLocation())
            .appointmentSubject(action.getAppointmentSubject())
            .createdAt(action.getCreatedAt())
            .updatedAt(action.getUpdatedAt())
            .build();
    }

    private void updateAppointmentAfterHcmAcceptance(Long appointmentId, LocalDateTime acceptedDateTime, String hcmRemarks) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        appointment.setStatus(Appointment.AppointmentStatus.HCM_ACCEPTED);
        appointment.setHcmRemarks(hcmRemarks);
        appointment.setScheduledDateTime(acceptedDateTime);
        if (appointment.getScheduledDurationMinutes() == null) {
            appointment.setScheduledDurationMinutes(30);
        }
        appointmentRepository.save(appointment);
    }
}
