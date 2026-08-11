package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.DirectionFollowUp;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateDirectionFollowUpRequest {
    private Long appointmentId;
    private Long departmentId;
    private String responsibleOfficerName;
    private String instruction;
    private LocalDate dueDate;
    private DirectionFollowUp.Priority priority;
    private Boolean evidenceRequired;
}
