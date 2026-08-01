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

    private static final String DEFAULT_SUPER_ADMIN_USERNAME = "superadmin";
    private static final String LEGACY_SUPER_ADMIN_USERNAME = "superaadmin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${meghaconnect.bootstrap.super-admin-username:${MEGHACONNECT_SUPER_ADMIN_USERNAME:superadmin}}")
    private String configuredUsername;

    @Value("${meghaconnect.bootstrap.super-admin-password:${MEGHACONNECT_SUPER_ADMIN_PASSWORD:${MEGHACONNECT_SUPER_ADMIN_INITIAL_PASSWORD:}}}")
    private String configuredInitialPassword;

    @Value("${meghaconnect.bootstrap.super-admin-password-repair-enabled:${MEGHACONNECT_SUPER_ADMIN_PASSWORD_REPAIR_ENABLED:false}}")
    private boolean passwordRepairEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String superAdminUsername = trimToNull(configuredUsername);
        if (superAdminUsername == null) {
            superAdminUsername = DEFAULT_SUPER_ADMIN_USERNAME;
        }

        String initialPassword = trimToNull(configuredInitialPassword);

        java.util.List<User> canonicalUsers = userRepository.findAllByNormalizedUsername(superAdminUsername);
        java.util.List<User> legacyUsers = LEGACY_SUPER_ADMIN_USERNAME.equalsIgnoreCase(superAdminUsername)
                ? java.util.List.of()
                : userRepository.findAllByNormalizedUsername(LEGACY_SUPER_ADMIN_USERNAME);

        boolean newAccount = canonicalUsers.isEmpty() && legacyUsers.isEmpty();
        User user = !canonicalUsers.isEmpty()
                ? canonicalUsers.get(0)
                : !legacyUsers.isEmpty()
                    ? legacyUsers.get(0)
                    : User.builder()
                        .username(superAdminUsername)
                        .fullName("Super Admin")
                        .role(User.UserRole.SUPER_ADMIN)
                        .active(true)
                        .offlineAccess(false)
                        .locked(false)
                        .build();

        if (user.getId() == null && initialPassword == null) {
            log.warn("Super Admin bootstrap skipped account creation because no initial password is configured.");
            return;
        }

        if (!superAdminUsername.equals(user.getUsername())) {
            log.info("Super Admin bootstrap migrating username from legacy value to canonical username={}", superAdminUsername);
            user.setUsername(superAdminUsername);
        }

        canonicalUsers.stream()
                .filter(duplicate -> duplicate != user)
                .forEach(duplicate -> deactivateDuplicate(duplicate, "canonical-duplicate"));
        legacyUsers.stream()
                .filter(duplicate -> duplicate != user)
                .forEach(duplicate -> deactivateDuplicate(duplicate, "legacy-duplicate"));

        String currentHash = trimToNull(user.getPasswordHash());
        boolean credentialsInitialized = false;
        if (currentHash == null && initialPassword == null) {
            log.warn("Super Admin bootstrap cannot create or repair password because no initial password is configured.");
        } else if (currentHash == null) {
            user.setPasswordHash(passwordEncoder.encode(initialPassword));
            credentialsInitialized = true;
            log.info("Super Admin bootstrap initialized password hash for username={}", superAdminUsername);
        } else if (initialPassword != null && passwordRepairEnabled && !passwordMatches(initialPassword, currentHash)) {
            user.setPasswordHash(passwordEncoder.encode(initialPassword));
            credentialsInitialized = true;
            log.warn("Super Admin bootstrap repaired password hash for username={} because controlled repair is enabled.", superAdminUsername);
        } else if (initialPassword != null && !passwordMatches(initialPassword, currentHash)) {
            log.warn("Super Admin password did not match configured initial password; leaving existing hash unchanged because repair is disabled.");
        }
        user.setRole(User.UserRole.SUPER_ADMIN);
        if (trimToNull(user.getFullName()) == null) {
            user.setFullName("Super Admin");
        }
        user.setDepartment(null);
        user.setActive(true);
        if (newAccount || credentialsInitialized) {
            user.setLocked(false);
            user.setPasswordChangeRequired(true);
            user.setTemporaryPasswordCreatedAt(com.survisha.meghaconnect.util.DateTimeUtil.nowIST());
        }
        user.setUpdatedBy("super-admin-bootstrap");
        if (user.getCreatedBy() == null) {
            user.setCreatedBy("super-admin-bootstrap");
        }
        userRepository.save(user);
        log.info("Super Admin bootstrap completed for username={}", superAdminUsername);
    }

    private void deactivateDuplicate(User duplicate, String reason) {
        String archivedUsername = duplicate.getUsername() + "-inactive-" + duplicate.getId();
        if (archivedUsername.length() > 100) {
            archivedUsername = archivedUsername.substring(0, 100);
        }
        duplicate.setUsername(archivedUsername);
        duplicate.setActive(false);
        duplicate.setLocked(true);
        duplicate.setDepartment(null);
        duplicate.setUpdatedBy("super-admin-bootstrap");
        userRepository.save(duplicate);
        log.warn("Super Admin bootstrap deactivated {} account id={}", reason, duplicate.getId());
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, storedPassword);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
