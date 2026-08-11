package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Appointment;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class AppointmentReportFilter {
    private Long departmentId;
    private String scheme;
    private String constituency;
    private String district;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate toDate;
    private Appointment.AppointmentStatus appointmentStatus;
    private String followUpStatus;
    private String mla;
    private String agendaType;
    private String appointmentType;
    private Appointment.AppointmentCategory appointmentCategory;
    private Long routedDepartmentId;
    private String responsibleOfficer;
}
