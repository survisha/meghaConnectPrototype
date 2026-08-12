package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompletedAppointmentSummaryResponse {
    private Long appointmentId;
    private String applicationId;
    private String applicantName;
    private String epic;
    private String mobile;
    private Boolean photoAvailable;
    private String appointmentCategory;
    private String appointmentType;
    private String department;
    private String scheme;
    private String constituency;
    private String district;
    private String mla;
    private String agendaType;
    private LocalDateTime requestedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime meetingAt;
    private LocalDateTime completedAt;
    private String directionSummary;
    private String assignedDepartment;
    private String followUpStatus;
    private String responsibleOfficer;
    private LocalDate dueDate;
    private String status;
}
