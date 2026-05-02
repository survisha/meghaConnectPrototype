package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Appointment;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenAppointmentRequest {

    private Long applicantId;

    @NotNull(message = "eventType is required")
    private Appointment.EventType eventType;

    @NotBlank(message = "subject is required")
    private String subject;

    private String description;
    private String department;
    private String appointmentType;
    private String agendaType;
    private Appointment.MeetingLocation requestedLocation;
    private Boolean mlaMdcApproved;
    private Boolean walkIn;
}
