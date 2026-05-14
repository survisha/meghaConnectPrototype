package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotImportRowResultDto {
    private Integer rowNumber;
    private String srNo;
    private Boolean success;
    private String name;
    private String phoneNumber;
    private Long visitorId;
    private Long appointmentId;
    private String applicationId;
    private String message;
}
