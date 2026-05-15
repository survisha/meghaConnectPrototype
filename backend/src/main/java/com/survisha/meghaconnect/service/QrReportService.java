package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrScanAuditLogDto;
import com.survisha.meghaconnect.dto.RecentQrScanDto;
import com.survisha.meghaconnect.dto.VisitorMovementLogDto;
import com.survisha.meghaconnect.entity.QrScanAuditLog;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.entity.VisitorMovementLog;
import com.survisha.meghaconnect.repository.QrScanAuditLogRepository;
import com.survisha.meghaconnect.repository.VisitorMovementLogRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QrReportService {

    private final QrScanAuditLogRepository qrScanAuditLogRepository;
    private final VisitorMovementLogRepository visitorMovementLogRepository;
    private final VisitorRepository visitorRepository;

    public Page<QrScanAuditLogDto> getAuditLogs(Pageable pageable,
                                                LocalDate fromDate,
                                                LocalDate toDate,
                                                String gateName,
                                                QrScanAuditLog.ScanAction action,
                                                QrScanAuditLog.ScanStatus status,
                                                String scannedBy,
                                                String requestId) {
        return qrScanAuditLogRepository.findAll(
                        auditSpec(fromDate, toDate, gateName, action, status, scannedBy, requestId),
                        pageable)
                .map(this::toAuditDto);
    }

    public Page<VisitorMovementLogDto> getMovementReport(Pageable pageable,
                                                         LocalDate date,
                                                         String gateName,
                                                         String status) {
        return visitorMovementLogRepository.findAll(movementSpec(date, gateName, status), pageable)
                .map(this::toMovementDto);
    }

    public List<RecentQrScanDto> getRecentScans(String scannedBy) {
        return qrScanAuditLogRepository.findTop20ByScannedByOrderByCreatedAtDesc(scannedBy)
                .stream()
                .map(this::toRecentScanDto)
                .toList();
    }

    private Specification<QrScanAuditLog> auditSpec(LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String gateName,
                                                    QrScanAuditLog.ScanAction action,
                                                    QrScanAuditLog.ScanStatus status,
                                                    String scannedBy,
                                                    String requestId) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (fromDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }
            if (hasText(gateName)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("gateName")), like(gateName)));
            }
            if (action != null) {
                predicate = cb.and(predicate, cb.equal(root.get("action"), action));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (hasText(scannedBy)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("scannedBy")), like(scannedBy)));
            }
            if (hasText(requestId)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("requestId")), like(requestId)));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return predicate;
        };
    }

    private Specification<VisitorMovementLog> movementSpec(LocalDate date, String gateName, String status) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (date != null) {
                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(root.get("scanTime"), date.atStartOfDay()),
                        cb.lessThan(root.get("scanTime"), date.plusDays(1).atStartOfDay())
                );
            }
            if (hasText(gateName)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("gateName")), like(gateName)));
            }
            VisitorMovementLog.MovementType movementType = parseMovementType(status);
            if (movementType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("movementType"), movementType));
            }
            query.orderBy(cb.desc(root.get("scanTime")));
            return predicate;
        };
    }

    private QrScanAuditLogDto toAuditDto(QrScanAuditLog log) {
        return QrScanAuditLogDto.builder()
                .id(log.getId())
                .appointmentId(log.getAppointmentId())
                .visitorId(log.getVisitorId())
                .scannedBy(log.getScannedBy())
                .deviceId(log.getDeviceId())
                .gateName(log.getGateName())
                .action(log.getAction())
                .status(log.getStatus())
                .failureReason(log.getFailureReason())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestId(log.getRequestId())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private VisitorMovementLogDto toMovementDto(VisitorMovementLog movementLog) {
        Visitor visitor = movementLog.getVisitor();
        return VisitorMovementLogDto.builder()
                .id(movementLog.getId())
                .appointmentId(movementLog.getAppointment() != null ? movementLog.getAppointment().getId() : null)
                .visitorId(visitor != null ? visitor.getId() : null)
                .visitorName(visitor != null ? visitor.getFullName() : null)
                .movementType(movementLog.getMovementType())
                .status(movementLog.getMovementType() != null ? movementLog.getMovementType().name() : null)
                .scannedBy(movementLog.getScannedBy())
                .gateName(movementLog.getGateName())
                .deviceId(movementLog.getDeviceId())
                .scanTime(movementLog.getScanTime())
                .remarks(movementLog.getRemarks())
                .build();
    }

    private RecentQrScanDto toRecentScanDto(QrScanAuditLog log) {
        String visitorName = log.getVisitorId() != null
                ? visitorRepository.findById(log.getVisitorId()).map(Visitor::getFullName).orElse("Visitor")
                : "Visitor";
        return RecentQrScanDto.builder()
                .visitorName(visitorName)
                .scanTime(log.getCreatedAt())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .action(log.getAction())
                .appointmentId(log.getAppointmentId() != null ? log.getAppointmentId().toString() : null)
                .build();
    }

    private VisitorMovementLog.MovementType parseMovementType(String status) {
        if (!hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("CHECKED_IN".equals(normalized)) {
            normalized = "ENTRY";
        } else if ("CHECKED_OUT".equals(normalized)) {
            normalized = "EXIT";
        }
        try {
            return VisitorMovementLog.MovementType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
