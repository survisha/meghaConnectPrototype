package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentReportRow {
    private Long appointmentId;
    private String applicationId;
    private String applicantName;
    private String mobile;
    private String epicReference;
    private String constituency;
    private String district;
    private String appointmentCategory;
    private String appointmentType;
    private String agendaType;
    private String petitionSummary;
    private String status;
    private LocalDateTime scheduledDateTime;
    private LocalDateTime completedAt;
    private String meetingOutcome;
    private String directions;
    private String department;
    private String routedDepartment;
    private String responsibleOfficer;
    private String followUpStatus;
    private String scheme;
    private List<String> supportingDocuments;
}
