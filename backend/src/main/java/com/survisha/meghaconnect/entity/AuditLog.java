package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_user",   columnList = "username"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String entityType;

    private Long entityId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT")
    private String details;

    @Column(name = "username", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 50)
    private String ipAddress;
}
