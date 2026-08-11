package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_type_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTypeConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String typeCode;           // A1, A2, A3, A4, B1, B2

    @Column(nullable = false, length = 200)
    private String typeName;           // Cabinet Meetings, Events/Programmes, etc.

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String typeCategory;       // INDIVIDUAL, BATCH

    // ==================== TYPE DEFINITION ====================
    // Type A1, A2 - Travel time related
    @Column(nullable = false)
    private Boolean requiresTravel = false;

    @Column(nullable = false)
    private Integer travelTimeBefore = 0;

    @Column(nullable = false)
    private Integer travelTimeAfter = 0;

    @Column(nullable = false)
    private Boolean blockTimeIncludes = true;

    // Type A4 - Individual appointment limits
    @Column(nullable = false)
    private Boolean hasAppointmentLimit = false;

    @Column
    private Integer maxAppointmentLimit;

    @Column(nullable = false)
    private Boolean limitIsSacrosanct = true;

    @Column(nullable = false)
    private Boolean generateAlerts = false;

    // Type A3 - No travel characteristics
    @Column(nullable = false)
    private Boolean noTravelTime = false;

    // ==================== CALENDAR CONFIGURATION ====================
    @Column(length = 50)
    private String calendarColor;      // Hex color for UI (e.g., #FF5733)

    @Column(nullable = false)
    private Boolean blockCalendarSlot = true;  // Does this type block calendar time?

    @Column(length = 500)
    private String availabilityWindows; // JSON: {"mon": "09:00-17:00", "tue": "09:00-17:00"}

    @Column(columnDefinition = "TEXT")
    private String blackoutDates;      // JSON array of blackout date ranges

    // ==================== DIRECT SCHEDULING RULES ====================
    @Column(nullable = false)
    private Boolean allowDirectScheduling = false;  // Can non-admin directly schedule?

    @Column(length = 500)
    private String directSchedulingRoles; // Comma-separated: ADMIN,APPROVER,HCM

    @Column(nullable = false)
    private Boolean bypassApprovalProcess = false;  // Skip approval for direct scheduling?

    @Column(length = 500)
    private String approverBypassRoles;  // Roles that can bypass approval: ADMIN,APPROVER

    // ==================== CONFLICT HANDLING ====================
    @Column(nullable = false)
    private Boolean detectConflicts = true;

    @Column(nullable = false)
    private Boolean allowConflictOverride = false;  // Can conflicts be overridden?

    @Column(length = 500)
    private String conflictOverrideRoles; // Who can override: ADMIN,APPROVER

    @Column(nullable = false)
    private Boolean notifyOnConflict = true;

    @Column(columnDefinition = "TEXT")
    private String conflictNotificationTemplate; // Template for conflict notifications

    // ==================== BATCH SCHEDULING (TYPE B) ====================
    @Column(nullable = false)
    private Boolean isBatchType = false;

    @Column
    private Integer maxParticipants;        // For Public Durbar

    @Column(nullable = false)
    private Boolean requiresPreApproval = false;  // For B1 (Public Durbar)

    @Column(nullable = false)
    private Boolean allowWalkIn = false;         // For B2 (Public Walk-in)

    @Column
    private Integer maxWalkInCount;

    @Column(nullable = false)
    private Boolean autoAssignSeat = false;

    // ==================== WAITING LIST MANAGEMENT ====================
    @Column(nullable = false)
    private Boolean enableWaitingList = false;

    @Column
    private Integer maxWaitingListSize;

    @Column(nullable = false)
    private Boolean autoAddFromWaitingList = false;  // Auto add when slot freed?

    @Column
    private Integer waitingListNotificationDays;  // Notify X days before slot

    // ==================== APPROVER SPECIAL ROLE ====================
    @Column(nullable = false)
    private Boolean osdCanOverride = false;

    @Column(nullable = false)
    private Boolean osdCanBypassLimits = false;

    @Column(nullable = false)
    private Boolean osdCanDirectSchedule = false;

    // ==================== DRAG AND DROP SCHEDULING ====================
    @Column(nullable = false)
    private Boolean allowDragDropRescheduling = false;

    @Column(length = 500)
    private String dragDropAllowedRoles; // Who can drag-drop: ADMIN,APPROVER,HCM

    @Column(nullable = false)
    private Boolean validateConflictsOnDragDrop = true;

    // ==================== STATUS ====================
    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;
}
