package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RejectedAppointmentDetailResponse {
    private CompletedAppointmentDetailResponse.Applicant applicant;
    private CompletedAppointmentDetailResponse.AppointmentInfo appointment;
    private String petitionSummary;
    private List<CompletedAppointmentDetailResponse.DocumentItem> documents;
    private String rejectionReason;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String returnReason;
    private String requiredInformation;
    private List<CompletedAppointmentDetailResponse.StatusHistoryItem> statusHistory;
    private Boolean readOnly;
}
