package com.survisha.meghaconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestAppointmentResponse {
    private String referenceId;
    private String status;
    private String message;
}
