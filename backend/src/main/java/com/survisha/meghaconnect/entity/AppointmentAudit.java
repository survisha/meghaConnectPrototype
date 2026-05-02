package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_audit",
    indexes = {
        @Index(name = "idx_appt_audit_appointment", columnList = "appointment_id"),
        @Index(name = "idx_appt_audit_request", columnList = "requestId")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Appointment.AppointmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Appointment.AppointmentStatus newStatus;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(length = 50)
    private String performedRole;

    @Column(length = 128)
    private String requestId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
