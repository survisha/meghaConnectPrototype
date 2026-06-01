package com.survisha.meghaconnect.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_call_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCallAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "module_name", length = 80)
    private String moduleName;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "prompt_type", length = 80)
    private String promptType;

    @Column(length = 40)
    private String provider;

    @Column(length = 120)
    private String model;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
