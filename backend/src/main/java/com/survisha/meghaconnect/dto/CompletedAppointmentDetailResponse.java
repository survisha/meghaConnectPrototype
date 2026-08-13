package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompletedAppointmentDetailResponse {
    private Applicant applicant;
    private AppointmentInfo appointment;
    private String petitionSummary;
    private String approverRemarks;
    private String hcmRemarks;
    private String forwardedDepartment;
    private List<DirectionItem> directions;
    private List<ActionItem> actionItems;
    private List<DocumentItem> documents;
    private String aiSummary;
    private List<StatusHistoryItem> statusHistory;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Applicant {
        private Long id; private String name; private String epic; private String mobile;
        private String address; private String constituency; private String district; private String pincode;
        private Boolean photoAvailable;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppointmentInfo {
        private Long id; private String applicationId; private String category; private String type; private String source;
        private LocalDateTime requestedAt; private LocalDateTime scheduledAt; private LocalDateTime meetingAt; private LocalDateTime completedAt;
        private String department; private String scheme; private String mla; private String agendaType; private String purpose;
        private String meetingOutcome; private String status;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DirectionItem {
        private String directionId; private LocalDateTime date; private String direction; private String department;
        private String officer; private LocalDate dueDate; private String followUpStatus;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActionItem {
        private Long id; private String directionId; private String department; private String officer; private String instruction;
        private LocalDate dueDate; private String status; private Boolean evidenceRequired; private Boolean escalated;
        private LocalDateTime completedDate; private String completionRemarks;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentItem {
        private Long id; private String filename; private String documentType; private String contentType;
        private Long fileSizeBytes; private LocalDateTime uploadedDate; private String uploadedBy;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusHistoryItem {
        private String oldStatus; private String newStatus; private String action; private String remarks;
        private String performedBy; private String performedRole; private LocalDateTime timestamp;
    }
}
