package com.survisha.meghaconnect.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private Long id;
    private LocalDateTime timestamp;

    private String module;
    private String entity;
    private String entityType;
    private Long entityId;

    private String action;
    private String user;
    private String performedBy;
    private String role;
    private String userRole;

    private String requestId;
    private String oldValue;
    private String newValue;
    private String description;
    private String details;
    private String status;
    private String ipAddress;
    private String endpoint;
}
