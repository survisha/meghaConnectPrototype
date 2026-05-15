package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_movement_log",
        indexes = {
                @Index(name = "idx_visitor_movement_scan_time", columnList = "scan_time"),
                @Index(name = "idx_visitor_movement_gate", columnList = "gate_name"),
                @Index(name = "idx_visitor_movement_type", columnList = "movement_type"),
                @Index(name = "idx_visitor_movement_appointment", columnList = "appointment_id"),
                @Index(name = "idx_visitor_movement_visitor", columnList = "visitor_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorMovementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "scanned_by", nullable = false, length = 100)
    private String scannedBy;

    @Column(name = "gate_name", length = 150)
    private String gateName;

    @Column(name = "device_id", length = 150)
    private String deviceId;

    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime;

    @Column(length = 500)
    private String remarks;

    public enum MovementType {
        ENTRY,
        EXIT
    }
}
