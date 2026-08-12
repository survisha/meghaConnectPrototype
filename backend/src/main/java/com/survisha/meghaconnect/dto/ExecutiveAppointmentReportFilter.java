package com.survisha.meghaconnect.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ExecutiveAppointmentReportFilter {
    private String applicationId;
    private String applicantName;
    private String epic;
    private String mobile;
    private Long departmentId;
    private String department;
    private String scheme;
    private String constituency;
    private String district;
    private String status;
    private String followUpStatus;
    private String mla;
    private String agendaType;
    private String appointmentType;
    private String appointmentCategory;
    private String responsibleOfficer;
    private String rejectedBy;
    private String rejectionReason;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate toDate;
}
