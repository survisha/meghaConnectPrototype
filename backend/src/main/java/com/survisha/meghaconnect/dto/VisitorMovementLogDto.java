package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.VisitorMovementLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorMovementLogDto {

    private Long id;
    private Long appointmentId;
    private Long visitorId;
    private String visitorName;
    private VisitorMovementLog.MovementType movementType;
    private String status;
    private String scannedBy;
    private String gateName;
    private String deviceId;
    private LocalDateTime scanTime;
    private String remarks;
}
