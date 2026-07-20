package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_scan_audit_log",
        indexes = {
                @Index(name = "idx_qr_scan_audit_created_at", columnList = "created_at"),
                @Index(name = "idx_qr_scan_audit_request_id", columnList = "request_id"),
                @Index(name = "idx_qr_scan_audit_status_action", columnList = "status, action"),
                @Index(name = "idx_qr_scan_audit_gate", columnList = "gate_name"),
                @Index(name = "idx_qr_scan_audit_appointment", columnList = "appointment_id"),
                @Index(name = "idx_qr_scan_audit_visitor", columnList = "visitor_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScanAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "visitor_id")
    private Long visitorId;

    @Column(name = "scanned_by", nullable = false, length = 100)
    private String scannedBy;

    @Column(name = "device_id", length = 150)
    private String deviceId;

    @Column(name = "gate_name", length = 150)
    private String gateName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum ScanAction {
        VALIDATE,
        CHECK_IN,
        CHECK_OUT
    }

    public enum ScanStatus {
        SUCCESS,
        FAILED
    }
}
