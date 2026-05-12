package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.SchemeApplication;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeApplicationDto {
    private Long id;
    private Long applicantId;
    private VisitorDto applicant;
    private String applicantName;
    private Long appointmentId;
    private SchemeApplication.SchemeType schemeType;
    private String projectName;
    private String projectCategory;
    private String beneficiaryType;
    private String beneficiaryCount;
    private BigDecimal estimatedCost;
    private BigDecimal communityContribution;
    private String justification;
    private SchemeApplication.HcmDecision hcmDecision;
    private BigDecimal hcmApprovedCost;
    private String status;
    private List<SchemeApplicationItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
