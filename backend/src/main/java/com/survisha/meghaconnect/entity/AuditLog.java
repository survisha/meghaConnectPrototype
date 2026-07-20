package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_user",   columnList = "username"),
        @Index(name = "idx_audit_time",   columnList = "created_at"),
        @Index(name = "idx_audit_request", columnList = "request_id"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT")
    private String details;

    @Column(name = "username", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_role", length = 50)
    private String role;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 30)
    private String status;

    @Column(length = 300)
    private String endpoint;

    @Column(length = 50)
    private String ipAddress;
}
