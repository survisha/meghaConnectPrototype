package com.survisha.meghaconnect.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Disabled production-safe placeholder. Password hash generation must be done
 * through controlled admin tooling, not hardcoded demo credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meghaconnect.security.password-hash-generator", name = "enabled", havingValue = "true")
public class PasswordHashGenerator {
    
    @EventListener(ApplicationReadyEvent.class)
    public void generateHashes() {
        log.warn("PasswordHashGenerator is enabled, but hardcoded demo credential generation has been removed. Use approved admin tooling instead.");
    }
}
