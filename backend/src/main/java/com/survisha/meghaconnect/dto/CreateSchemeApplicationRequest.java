package com.survisha.meghaconnect.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSchemeApplicationRequest {
    private Long applicantId;
    private String schemeType;
    private String projectName;
    private String projectCategory;
    private String beneficiaryType;
    private String beneficiaryCount;
    private BigDecimal estimatedCost;
    private BigDecimal communityContribution;
    private String justification;
    private List<SchemeApplicationItemDto> items;
}
