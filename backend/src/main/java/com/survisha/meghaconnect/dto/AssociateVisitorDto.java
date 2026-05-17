package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociateVisitorDto {
    private Long id;
    private Long citizenId;
    private String fullName;
    private String mobileNumber;
    private String epicReference;
    private String aadhaarReference;
    private String addressSummary;
    private String photoUrl;
    private String kycStatus;
    private String status;
    private String relationship;
    private String remarks;
    private String role;
    private LocalDateTime createdAt;
}
