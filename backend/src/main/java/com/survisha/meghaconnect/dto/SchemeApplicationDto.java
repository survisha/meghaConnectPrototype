package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.SchemeApplication;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeApplicationDto {
    private Long id;
    private Long applicantId;
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
}
