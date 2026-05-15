package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.QrScanAuditLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScanAuditLogDto {

    private Long id;
    private Long appointmentId;
    private Long visitorId;
    private String scannedBy;
    private String deviceId;
    private String gateName;
    private QrScanAuditLog.ScanAction action;
    private QrScanAuditLog.ScanStatus status;
    private String failureReason;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private LocalDateTime createdAt;
}
