package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.DirectionFollowUp;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DirectionFollowUpDto {
    private Long id;
    private String directionId;
    private Long appointmentId;
    private Long visitorId;
    private Long departmentId;
    private String departmentName;
    private String responsibleOfficerName;
    private String instruction;
    private LocalDate dueDate;
    private String status;
    private DirectionFollowUp.Priority priority;
    private Boolean evidenceRequired;
    private long daysOverdue;
    private LocalDateTime completedDate;
    private String completionRemarks;
    private LocalDateTime createdAt;
    private String createdBy;
}
