package com.survisha.meghaconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores temporary OTPs issued to visitors during mobile-based login.
 * Each record is valid for a short window (default 5 minutes).
 * Expired or consumed records must be purged periodically.
 */
@Entity
@Table(name = "visitor_otp_temp",
    indexes = {
        @Index(name = "idx_otp_phone", columnList = "phone_number"),
        @Index(name = "idx_otp_expiry", columnList = "expires_at"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** True once this OTP has been successfully verified (prevents replay). */
    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    /** Number of failed attempts against this OTP record. */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
