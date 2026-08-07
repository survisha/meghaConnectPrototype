package com.survisha.meghaconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RedisConfigurationValidator {
    private final RedisCacheProperties cacheProperties;
    private final RedisProperties redisProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        if (!cacheProperties.isEnabled()) {
            return;
        }
        if (redisProperties.getHost() == null || redisProperties.getHost().isBlank()) {
            throw new IllegalStateException("REDIS_HOST must be configured when Redis is enabled");
        }
        if (redisProperties.getPort() < 1 || redisProperties.getPort() > 65535) {
            throw new IllegalStateException("REDIS_PORT must be between 1 and 65535");
        }
        if (redisProperties.getTimeout() == null || redisProperties.getTimeout().isZero()
                || redisProperties.getTimeout().isNegative()) {
            throw new IllegalStateException("REDIS_COMMAND_TIMEOUT must be positive");
        }
        boolean protectedProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "uat".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile));
        if (protectedProfile && (redisProperties.getPassword() == null
                || redisProperties.getPassword().isBlank())) {
            throw new IllegalStateException("REDIS_PASSWORD is required in UAT and production");
        }
        validateSecurityTtl("OTP", cacheProperties.getOtpTtl());
        validateSecurityTtl("CAPTCHA", cacheProperties.getCaptchaTtl());
        if (protectedProfile && cacheProperties.isSecurityStoreFallbackEnabled()) {
            throw new IllegalStateException("Redis security-store fallback must remain disabled in UAT and production");
        }
    }

    private void validateSecurityTtl(String name, Duration ttl) {
        if (ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalStateException(name + " TTL must be between 1 and 15 minutes");
        }
    }
}
