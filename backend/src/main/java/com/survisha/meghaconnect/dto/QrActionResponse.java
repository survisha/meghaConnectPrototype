package com.survisha.meghaconnect.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrActionResponse {

    private boolean success;
    private String message;
    private String action;
    private String status;
    private String movementStatus;
    private Long appointmentId;
    private Long visitorId;
    private LocalDateTime scanTime;
}
