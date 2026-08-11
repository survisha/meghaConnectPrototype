package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 20)
    private String phoneNumber;

    private boolean active = true;
    private boolean offlineAccess = false;
    private boolean locked = false;
    private boolean passwordChangeRequired = false;

    private int failedLoginAttempts;
    private LocalDateTime lastFailedLoginAt;
    private LocalDateTime lockedAt;
    @Column(length = 200)
    private String lockReason;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime temporaryPasswordCreatedAt;
    private long credentialsVersion;
    @Column(length = 100)
    private String unlockedBy;
    private LocalDateTime unlockedAt;

    private LocalDateTime lastLogin;

    // Delegate authority (for Jt Secy)
    private Long delegatedToUserId;
    private LocalDateTime delegationExpiresAt;

    public enum UserRole {
        SUPER_ADMIN, DEPARTMENT_ADMIN, DEO, DEPARTMENT_PA, HEAD_DEPARTMENT,
        HCM, ADMIN, APPROVER, SECURITY, PUBLIC, CITIZEN
    }
}
