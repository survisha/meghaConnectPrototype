package com.survisha.meghaconnect.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility to generate BCrypt password hashes for demo users
 * Prints hashes to console on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meghaconnect.security.password-hash-generator", name = "enabled", havingValue = "true")
public class PasswordHashGenerator {
    
    private final PasswordEncoder passwordEncoder;
    
    @EventListener(ApplicationReadyEvent.class)
    public void generateHashes() {
        String[][] credentials = {
            {"hcm", "hcm123"},
            {"admin", "admin123"},
            {"saidul", "osd123"},
            {"jtsecy", "jts123"},
            {"cmo", "cmo123"},
            {"deo1", "deo123"},
            {"public1", "public123"}
        };

        for (String[] cred : credentials) {
            passwordEncoder.encode(cred[1]);
        }

        log.info("Demo password hash generation completed for {} users. Hash values are intentionally not written to application logs.", credentials.length);
    }
}
