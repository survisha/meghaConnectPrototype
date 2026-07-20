package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_qr_token",
        indexes = {
                @Index(name = "idx_qr_token_appointment", columnList = "appointment_id"),
                @Index(name = "idx_qr_token_visitor", columnList = "visitor_id"),
                @Index(name = "idx_qr_token_status_validity", columnList = "status, valid_from, valid_to")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentQrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id")
    private Long departmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QrStatus status;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "generated_by", nullable = false, length = 100)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_in_by", length = 100)
    private String checkedInBy;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "checked_out_by", length = 100)
    private String checkedOutBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "gate_name", length = 150)
    private String gateName;

    @Column(name = "device_id", length = 150)
    private String deviceId;

    public enum QrStatus {
        QR_GENERATED,
        ACTIVE,
        CHECKED_IN,
        CHECKED_OUT,
        EXPIRED,
        CANCELLED,
        REVOKED
    }
}
