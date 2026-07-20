package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final String SUPER_ADMIN_USERNAME = "superaadmin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${meghaconnect.bootstrap.super-admin-password:${MEGHACONNECT_SUPER_ADMIN_PASSWORD:${MEGHACONNECT_SUPER_ADMIN_INITIAL_PASSWORD:Megha@TW26}}}")
    private String configuredInitialPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String initialPassword = trimToNull(configuredInitialPassword);
        if (initialPassword == null) {
            log.info("Super Admin bootstrap skipped because no initial password is configured.");
            return;
        }

        User user = userRepository.findByNormalizedUsername(SUPER_ADMIN_USERNAME)
                .orElseGet(() -> User.builder()
                        .username(SUPER_ADMIN_USERNAME)
                        .fullName("Super Admin")
                        .role(User.UserRole.SUPER_ADMIN)
                        .active(true)
                        .offlineAccess(false)
                        .locked(false)
                        .build());

        if (!SUPER_ADMIN_USERNAME.equals(user.getUsername())) {
            user.setUsername(SUPER_ADMIN_USERNAME);
        }
        String currentHash = user.getPasswordHash();
        if (currentHash == null || !passwordEncoder.matches(initialPassword, currentHash)) {
            user.setPasswordHash(passwordEncoder.encode(initialPassword));
            log.info("Super Admin bootstrap repaired password hash for username={}", SUPER_ADMIN_USERNAME);
        }
        user.setRole(User.UserRole.SUPER_ADMIN);
        user.setDepartment(null);
        user.setActive(true);
        user.setLocked(false);
        user.setPasswordChangeRequired(true);
        user.setUpdatedBy("super-admin-bootstrap");
        if (user.getCreatedBy() == null) {
            user.setCreatedBy("super-admin-bootstrap");
        }
        userRepository.save(user);
        log.info("Super Admin bootstrap completed for username={}", SUPER_ADMIN_USERNAME);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
