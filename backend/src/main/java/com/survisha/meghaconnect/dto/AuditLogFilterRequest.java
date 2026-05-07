package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogFilterRequest {
    private String module;
    private String action;
    private String user;
    private String role;
    private String requestId;
    private String status;
    private String from;
    private String to;
}
