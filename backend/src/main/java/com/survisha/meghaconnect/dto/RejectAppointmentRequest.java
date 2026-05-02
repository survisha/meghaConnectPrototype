package com.survisha.meghaconnect.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectAppointmentRequest {

    @NotBlank(message = "reason is required")
    private String reason;
}
