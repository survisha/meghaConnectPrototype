package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RejectedAppointmentSummaryResponse {
    private Long appointmentId;
    private String applicationId;
    private String applicantName;
    private String epic;
    private String mobile;
    private String department;
    private String scheme;
    private String constituency;
    private String district;
    private String mla;
    private String agendaType;
    private String appointmentType;
    private LocalDateTime requestedAt;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectionReason;
    private String status;
}
