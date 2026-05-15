package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Appointment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentWorkflowResponse {

    private Long id;
    private String applicationId;
    private Long applicantId;
    private String applicantName;
    private String applicantMobile;
    private String district;
    private String department;
    private String subject;
    private String description;
    private String appointmentType;
    private String agendaType;
    private Appointment.EventType eventType;
    private Appointment.AppointmentStatus status;
    private Appointment.MeetingLocation requestedLocation;
    private LocalDateTime scheduledDateTime;
    private Integer scheduledDurationMinutes;
    private Long publicDarbarId;
    private LocalDate publicDarbarDate;
    private String publicDarbarLocation;
    private Integer publicDarbarTokenNumber;
    private String rejectionReason;
    private String qrToken;
    private String qrStatus;
    private LocalDateTime qrValidFrom;
    private LocalDateTime qrValidTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
