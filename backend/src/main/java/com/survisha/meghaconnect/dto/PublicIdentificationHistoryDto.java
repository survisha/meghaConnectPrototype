package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicIdentificationHistoryDto {
    private Long citizenId;
    private String citizenName;
    private String photoUrl;
    private long visitCount;
    private LocalDateTime lastVisitedAt;
    private List<SchemeHistoryItem> schemes;
    private List<AppointmentHistoryItem> appointments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchemeHistoryItem {
        private Long id;
        private String schemeName;
        private String projectName;
        private LocalDateTime appliedDate;
        private String status;
        private BigDecimal amount;
        private String remarks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentHistoryItem {
        private Long appointmentId;
        private String applicationId;
        private LocalDateTime dateTime;
        private String department;
        private String officerName;
        private String purpose;
        private String status;
        private String remarks;
    }
}
