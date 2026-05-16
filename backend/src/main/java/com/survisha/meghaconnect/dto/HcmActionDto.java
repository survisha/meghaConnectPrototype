package com.survisha.meghaconnect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for HCM Actions on appointments/meetings
 * Supports gesture-based interactions: right swipe (accept/modify), left swipe (reject/delay)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HcmActionDto {
    
    private Long id;
    
    private Long appointmentId;
    
    // Action type: ACCEPT, ACCEPT_WITH_CHANGES, MARK_IMPORTANT, SNOOZE, REJECT
    private String actionType;
    
    // Action status: PENDING, CONFIRMED, COMPLETED
    private String actionStatus;
    
    // For ACCEPT: date/time confirmation
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptedDateTime;
    
    // For MARK_IMPORTANT
    private Boolean isImportantMeeting;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedEarlierDateTime;
    
    // For SNOOZE
    private String snoozeType; // DAYS_7, DAYS_15, DAYS_30, CUSTOM
    private Integer snoozeDurationDays;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime snoozedUntil;
    
    // For REJECT
    private Boolean isRejected;
    private String clarificationRequested;
    
    // HCM remarks
    private String hcmRemarks;
    private String decision;
    private String departmentCode;
    private String departmentName;
    private String createdBy;
    private String createdByRole;
    
    // Gesture info
    private String gestureType; // RIGHT_SWIPE, LEFT_SWIPE
    
    // Original appointment details
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalDateTime;
    
    private String originalLocation;
    private String appointmentSubject;
    
    // Audit
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
