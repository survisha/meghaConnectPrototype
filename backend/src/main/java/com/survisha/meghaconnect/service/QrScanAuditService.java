package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.QrScanAuditLog;
import com.survisha.meghaconnect.repository.QrScanAuditLogRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrScanAuditService {

    private final QrScanAuditLogRepository qrScanAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tokenHash,
                       Long appointmentId,
                       Long visitorId,
                       String scannedBy,
                       String deviceId,
                       String gateName,
                       QrScanAuditLog.ScanAction action,
                       QrScanAuditLog.ScanStatus status,
                       String failureReason,
                       String ipAddress,
                       String userAgent) {
        QrScanAuditLog log = QrScanAuditLog.builder()
                .tokenHash(limit(tokenHash, 64))
                .appointmentId(appointmentId)
                .visitorId(visitorId)
                .scannedBy(limit(firstNonBlank(scannedBy, "anonymous"), 100))
                .deviceId(limit(deviceId, 150))
                .gateName(limit(gateName, 150))
                .action(action)
                .status(status)
                .failureReason(limit(RequestContextUtil.sanitizeForLog(failureReason), 500))
                .ipAddress(limit(ipAddress, 100))
                .userAgent(limit(userAgent, 500))
                .requestId(limit(RequestContextUtil.getRequestId(), 128))
                .createdAt(DateTimeUtil.nowIST())
                .build();
        qrScanAuditLogRepository.save(log);
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
