package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.QrScanAuditLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentQrScanDto {

    private String visitorName;
    private LocalDateTime scanTime;
    private String status;
    private QrScanAuditLog.ScanAction action;
    private String appointmentId;
}
