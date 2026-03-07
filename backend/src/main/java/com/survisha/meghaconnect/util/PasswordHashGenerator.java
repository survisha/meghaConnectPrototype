package com.survisha.meghaconnect.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PasswordHashGenerator {
    
    private final PasswordEncoder passwordEncoder;
    
    @EventListener(ApplicationReadyEvent.class)
    public void generateHashes() {
        log.info("===============================================");
        log.info("BCRYPT PASSWORD HASHES FOR DEMO USERS");
        log.info("Copy these into V10__fix_user_passwords.sql");
        log.info("===============================================");
        
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
            String username = cred[0];
            String password = cred[1];
            String hash = passwordEncoder.encode(password);
            log.info("UPDATE users SET password_hash = '{}' WHERE username = '{}';  -- {}", hash, username, password);
        }
        
        log.info("===============================================");
    }
}
