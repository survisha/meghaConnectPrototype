package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Appointment;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentDto {
    private Long id;
    private String applicationId;
    private Long applicantId;
    private VisitorDto applicant;
    private String applicantName;
    private String applicantPhone;
    private Appointment.EventType eventType;
    private String subject;
    private String department;
    private String appointmentType;
    private String agendaType;
    private String agendaBrief;
    private Appointment.AppointmentStatus status;
    private Appointment.MeetingLocation requestedLocation;
    private LocalDateTime scheduledDateTime;
    private Integer scheduledDurationMinutes;
    private Boolean mlaMdcApproved;
    private String cmoRemarks;
    private String approverRemarks;
    private String hcmRemarks;
    private String shortNotes;
    private Boolean isWalkIn;
    private Integer meetingCountLast6Months;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
