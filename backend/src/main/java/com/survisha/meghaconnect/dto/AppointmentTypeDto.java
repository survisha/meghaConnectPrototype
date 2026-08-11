package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTypeDto {

    private Long id;
    private String typeCode;           // A1, A2, A3, A4, B1, B2
    private String typeName;
    private String description;
    private String typeCategory;       // INDIVIDUAL, BATCH
    
    // ==================== TYPE DEFINITION ====================
    private Boolean requiresTravel;
    private Integer travelTimeBefore;
    private Integer travelTimeAfter;
    private Boolean blockTimeIncludes;
    
    private Boolean hasAppointmentLimit;
    private Integer maxAppointmentLimit;
    private Boolean limitIsSacrosanct;
    private Boolean generateAlerts;
    
    private Boolean noTravelTime;

    // ==================== CALENDAR CONFIGURATION ====================
    private String calendarColor;       // Hex color for UI
    private Boolean blockCalendarSlot;
    private String availabilityWindows; // JSON
    private String blackoutDates;       // JSON array

    // ==================== DIRECT SCHEDULING RULES ====================
    private Boolean allowDirectScheduling;
    private String directSchedulingRoles;
    private Boolean bypassApprovalProcess;
    private String approverBypassRoles;

    // ==================== CONFLICT HANDLING ====================
    private Boolean detectConflicts;
    private Boolean allowConflictOverride;
    private String conflictOverrideRoles;
    private Boolean notifyOnConflict;
    private String conflictNotificationTemplate;

    // ==================== BATCH SCHEDULING (TYPE B) ====================
    private Boolean isBatchType;
    private Integer maxParticipants;
    private Boolean requiresPreApproval;
    private Boolean allowWalkIn;
    private Integer maxWalkInCount;
    private Boolean autoAssignSeat;

    // ==================== WAITING LIST MANAGEMENT ====================
    private Boolean enableWaitingList;
    private Integer maxWaitingListSize;
    private Boolean autoAddFromWaitingList;
    private Integer waitingListNotificationDays;

    // ==================== APPROVER SPECIAL ROLE ====================
    private Boolean osdCanOverride;
    private Boolean osdCanBypassLimits;
    private Boolean osdCanDirectSchedule;

    // ==================== DRAG AND DROP SCHEDULING ====================
    private Boolean allowDragDropRescheduling;
    private String dragDropAllowedRoles;
    private Boolean validateConflictsOnDragDrop;

    // ==================== STATUS ====================
    private Boolean isActive;
    private Integer displayOrder;
    private String createdBy;
    private String updatedBy;
}
