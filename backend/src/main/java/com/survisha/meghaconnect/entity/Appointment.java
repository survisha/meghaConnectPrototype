package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "appointments",
    indexes = {
        @Index(name = "idx_appt_appid", columnList = "applicationId"),
        @Index(name = "idx_appt_status", columnList = "status"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String applicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Visitor applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(length = 200)
    private String agendaType;

    @Column(columnDefinition = "TEXT")
    private String agendaBrief;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private MeetingLocation requestedLocation;

    private LocalDateTime scheduledDateTime;
    private Integer scheduledDurationMinutes;

    private Boolean mlaMdcApproved;

    @Column(columnDefinition = "TEXT")
    private String cmoRemarks;

    @Column(columnDefinition = "TEXT")
    private String approverRemarks;

    @Column(columnDefinition = "TEXT")
    private String hcmRemarks;

    @Column(columnDefinition = "TEXT")
    private String shortNotes;

    // ── AI-generated fields (R004–R007) ─────────────────────────────────────

    /** AI-generated document summary (R005) */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /** JSON object of AI-extracted fields from uploaded document (R004) */
    @Column(name = "ai_extracted_fields", columnDefinition = "TEXT")
    private String aiExtractedFields;

    /** AI-recommended meeting priority: HIGH, MEDIUM, or LOW (R007) */
    @Column(name = "ai_priority_level", length = 10)
    private String aiPriorityLevel;

    /** True if AI detected a possible duplicate application (R006) */
    @Column(name = "ai_duplicate_flag")
    private Boolean aiDuplicateFlag = false;

    private Boolean isWalkIn = false;

    // Snooze
    private LocalDateTime snoozedUntil;

    // Repeat tracking
    @Column(name = "meeting_count_last6_months")
    private Integer meetingCountLast6Months;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Direction> directions;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssociateMapping> associates;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_event_id")
    private ScheduleEvent scheduleEvent;

    public enum EventType  { A1, A2, A3, A4, B1, B2 }
    public enum MeetingLocation { SHILLONG, TURA, DELHI, OTHERS }

    public enum AppointmentStatus {
        SUBMITTED, DEO_PROCESSED, CMO_REVIEW, APPROVER_REVIEW,
        HCM_PENDING, HCM_ACCEPTED, HCM_SNOOZED, HCM_REJECTED,
        SCHEDULED, COMPLETED, CANCELLED
    }
}
