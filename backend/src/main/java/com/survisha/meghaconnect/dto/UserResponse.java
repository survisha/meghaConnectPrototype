package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private User.UserRole role;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private String phoneNumber;
    private boolean active;
    private boolean locked;
    private boolean offlineAccess;
    private boolean passwordChangeRequired;
    private int failedLoginAttempts;
    private LocalDateTime lastFailedLoginAt;
    private LocalDateTime lockedAt;
    private String lockReason;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime temporaryPasswordCreatedAt;
    private String unlockedBy;
    private LocalDateTime unlockedAt;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
