package com.survisha.meghaconnect.entity;

import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * HcmAction Entity - Tracks all actions taken by HCM on appointments/meetings
 * Supports gesture-based interactions: right swipe (accept/modify), left swipe (reject/delay)
 */
@Entity
@Table(name = "hcm_actions", indexes = {
    @Index(name = "idx_appointment", columnList = "appointment_id"),
    @Index(name = "idx_action_type", columnList = "action_type"),
    @Index(name = "idx_status", columnList = "action_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HcmAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Relationship to appointment
    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;
    
    // Action type: ACCEPT, ACCEPT_WITH_CHANGES, MARK_IMPORTANT, SNOOZE, REJECT
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
    
    // Action status: PENDING, CONFIRMED, COMPLETED
    @Column(name = "action_status", nullable = false, length = 50)
    private String actionStatus; // PENDING, CONFIRMED, COMPLETED
    
    // For ACCEPT: date/time confirmation
    @Column(name = "accepted_date_time")
    private LocalDateTime acceptedDateTime;
    
    // For MARK_IMPORTANT: earlier scheduling request
    @Column(name = "is_important_meeting")
    private Boolean isImportantMeeting;
    
    @Column(name = "requested_earlier_datetime")
    private LocalDateTime requestedEarlierDateTime;
    
    // For SNOOZE: duration and new scheduled date
    @Column(name = "snooze_type", length = 50) // DAYS_7, DAYS_15, DAYS_30, CUSTOM
    private String snoozeType;
    
    @Column(name = "snooze_duration_days")
    private Integer snoozeDurationDays;
    
    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;
    
    // For REJECT: clarification request
    @Column(name = "is_rejected")
    private Boolean isRejected;
    
    @Column(name = "clarification_requested", columnDefinition = "TEXT")
    private String clarificationRequested;
    
    // Remarks from HCM
    @Column(name = "hcm_remarks", columnDefinition = "TEXT")
    private String hcmRemarks;

    @Column(name = "decision", length = 200)
    private String decision;

    @Column(name = "department_code", length = 100)
    private String departmentCode;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_by_role", length = 50)
    private String createdByRole;
    
    // Gesture info
    @Column(name = "gesture_type", length = 50) // RIGHT_SWIPE, LEFT_SWIPE
    private String gestureType;
    
    // Original appointment details snapshot
    @Column(name = "original_datetime")
    private LocalDateTime originalDateTime;
    
    @Column(name = "original_location", length = 255)
    private String originalLocation;
    
    @Column(name = "appointment_subject", length = 300)
    private String appointmentSubject;
    
    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = DateTimeUtil.nowIST();
        updatedAt = DateTimeUtil.nowIST();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = DateTimeUtil.nowIST();
    }
}
