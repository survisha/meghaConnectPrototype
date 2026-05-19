package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentMultipartRequest {

    private Long applicantId;
    private String applicantName;
    private String applicantPhone;
    private String epicNumber;
    private String eventType;
    private Boolean isWalkIn;
    private String agendaType;
    private String agendaBrief;
    private String requestedLocation;
    private Boolean mlaMdcApproved;
    private String applicationType;
    private String schemeType;
    private String projectName;
    private String projectCategory;
    private String beneficiaryType;
    private String beneficiaryCount;
    private String estimatedCost;
    private String communityContribution;
    private String justification;
    private String organizationSubType;
    private String schemeHistoryList;
    private String associates;
    private String aiSummary;
    private String aiPriorityLevel;
    private String registrationAgendaType;
    private String registrationBriefDescription;
}
