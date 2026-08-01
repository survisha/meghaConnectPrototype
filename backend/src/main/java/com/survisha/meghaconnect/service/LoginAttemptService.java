package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

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
}
