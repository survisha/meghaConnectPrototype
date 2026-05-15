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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(length = 20)
    private String phoneNumber;

    private boolean active = true;
    private boolean offlineAccess = false;

    private LocalDateTime lastLogin;

    // Delegate authority (for Jt Secy)
    private Long delegatedToUserId;
    private LocalDateTime delegationExpiresAt;

    public enum UserRole {
        HCM, ADMIN, OSD, APPROVER, CMO_OFFICER, CMO, DATA_ENTRY_OPERATOR, SECURITY, PUBLIC, CITIZEN
    }
}
