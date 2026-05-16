package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDate;
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

    @Column(length = 300)
    private String subject;

    @Column(length = 150)
    private String department;

    @Column(length = 50)
    private String appointmentType;

    @Column(length = 20)
    private String appointmentSource;

    @Column(length = 40)
    private String guestReferenceId;

    @Column(length = 200)
    private String guestName;

    @Column(length = 20)
    private String guestMobile;

    @Column(length = 500)
    private String guestAddress;

    @Column(length = 150)
    private String guestEmail;

    @Column(length = 200)
    private String organizationName;

    @Column(length = 100)
    private String guestDesignation;

    @Column(length = 100)
    private String visitorCategory;

    @Column(length = 100)
    private String referredOffice;

    @Column(length = 200)
    private String referredByName;

    @Column(length = 500)
    private String reasonForAppointment;

    private LocalDate preferredDate;

    @Column(columnDefinition = "TEXT")
    private String agendaBrief;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private MeetingLocation requestedLocation;

    private LocalDateTime scheduledDateTime;
    private Integer scheduledDurationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_darbar_id")
    private PublicDarbar publicDarbar;

    private Integer publicDarbarTokenNumber;

    @Column(length = 100)
    private String approvedBy;

    @Column(length = 100)
    private String rejectedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(length = 100)
    private String selectedForPublicDarbarBy;

    private LocalDateTime selectedForPublicDarbarAt;

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
    @Builder.Default
    @Column(name = "ai_duplicate_flag", nullable = false)
    private Boolean aiDuplicateFlag = false;

    @Builder.Default
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
        CREATED, PENDING_APPROVER_REVIEW, FOLLOWUP, SELECTED_FOR_PUBLIC_DARBAR,
        PUBLIC_DARBAR_DATE_CREATED, SCHEDULED_FOR_PUBLIC_DARBAR,
        APPROVED, APPROVED_WITH_DATE_TIME, REJECTED,
        SUBMITTED, DEO_PROCESSED, CMO_REVIEW, APPROVER_REVIEW,
        HCM_PENDING, HCM_ACCEPTED, HCM_SNOOZED, HCM_REJECTED,
        SCHEDULED, FORWARDED_TO_DEPARTMENT, SUPPORTING_DOCUMENT_REQUIRED,
        COMPLETED, CANCELLED
    }
}
