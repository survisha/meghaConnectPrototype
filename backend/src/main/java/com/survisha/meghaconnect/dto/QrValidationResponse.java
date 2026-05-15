package com.survisha.meghaconnect.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrValidationResponse {

    private boolean valid;
    private Long appointmentId;
    private Long visitorId;
    private String visitorName;
    private String visitorPhotoUrl;
    private LocalDateTime appointmentDateTime;
    private String purpose;
    private String department;
    private String personToMeet;
    private String qrStatus;
    private String movementStatus;
    private String entryExitStatus;
    private Boolean canCheckIn;
    private Boolean canCheckOut;
    private String message;
}
