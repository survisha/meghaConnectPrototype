package com.survisha.meghaconnect.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveAppointmentRequest {

    @NotNull(message = "scheduledDateTime is required")
    private LocalDateTime scheduledDateTime;

    private Integer durationMinutes;
    private String remarks;
}
