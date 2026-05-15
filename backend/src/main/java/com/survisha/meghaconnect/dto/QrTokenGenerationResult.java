package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.AppointmentQrToken;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrTokenGenerationResult {

    private Long appointmentId;
    private Long visitorId;
    private String qrToken;
    private AppointmentQrToken.QrStatus status;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean newlyGenerated;
}
