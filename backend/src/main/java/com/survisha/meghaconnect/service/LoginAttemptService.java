package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.login.max-failed-attempts:3}")
    private int maxFailedAttempts;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(String username) {
        User user = userRepository.findForLoginUpdate(username).orElse(null);
        if (user == null || user.isLocked()) {
            return user != null && user.isLocked();
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        user.setLastFailedLoginAt(DateTimeUtil.nowIST());
        if (attempts >= maxFailedAttempts) {
            user.setLocked(true);
            user.setLockedAt(DateTimeUtil.nowIST());
            user.setLockReason("Maximum invalid password attempts exceeded");
            auditLogService.log("USER", user.getId(), "ACCOUNT_LOCKED",
                    "Account locked after consecutive invalid password attempts", "system");
        }
        userRepository.save(user);
        return user.isLocked();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        userRepository.findForLoginUpdate(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginAt(null);
            user.setLastLogin(DateTimeUtil.nowIST());
            userRepository.save(user);
        });
    }

    /**
     * Restores access to the sole global administrator only after independently
     * verifying the supplied password. Spring Security checks the locked flag
     * before checking credentials, so without this recovery path a locked Super
     * Admin cannot authenticate even with the correct password.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean unlockSuperAdminWithValidPassword(String username, String rawPassword) {
        User user = userRepository.findForLoginUpdate(username).orElse(null);
        if (user == null
                || !user.isLocked()
                || !user.isActive()
                || user.getRole() != User.UserRole.SUPER_ADMIN
                || rawPassword == null
                || !passwordMatches(rawPassword, user.getPasswordHash())) {
            return false;
        }

        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAt(null);
        user.setLockedAt(null);
        user.setLockReason(null);
        user.setUnlockedBy("verified-super-admin-login");
        user.setUnlockedAt(DateTimeUtil.nowIST());
        userRepository.save(user);
        auditLogService.log("USER", user.getId(), "ACCOUNT_UNLOCKED",
                "Super Admin account unlocked after successful credential verification", username);
        return true;
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
