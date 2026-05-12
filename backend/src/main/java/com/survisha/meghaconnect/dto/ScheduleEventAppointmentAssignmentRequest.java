package com.survisha.meghaconnect.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleEventAppointmentAssignmentRequest {
    private List<Long> appointmentIds;
    private String remarks;
}
