package com.survisha.meghaconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisConfigurationTest {

    @Test
    void defaultTtlsArePositive() {
        assertThat(new RedisCacheProperties().areTtlsPositive()).isTrue();
    }

    @Test
    void rejectsNonPositiveTtl() {
        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setSchemeTtl(Duration.ZERO);
        assertThat(properties.areTtlsPositive()).isFalse();
    }

    @Test
    void disabledRedisDoesNotRequireProductionPassword() {
        RedisCacheProperties cache = new RedisCacheProperties();
        RedisProperties redis = redis("localhost", "");
        new RedisConfigurationValidator(cache, redis, environment("prod")).validate();
    }

    @Test
    void enabledProductionRedisRequiresPassword() {
        RedisCacheProperties cache = new RedisCacheProperties();
        cache.setEnabled(true);
        RedisProperties redis = redis("localhost", "");

        assertThatThrownBy(() -> new RedisConfigurationValidator(cache, redis, environment("prod")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REDIS_PASSWORD");
    }

    @Test
    void acceptsSecureUatConfiguration() {
        RedisCacheProperties cache = new RedisCacheProperties();
        cache.setEnabled(true);
        RedisProperties redis = redis("redis", "strong-secret");
        new RedisConfigurationValidator(cache, redis, environment("uat")).validate();
    }

    private RedisProperties redis(String host, String password) {
        RedisProperties properties = new RedisProperties();
        properties.setHost(host);
        properties.setPassword(password);
        properties.setTimeout(Duration.ofSeconds(3));
        return properties;
    }

    private MockEnvironment environment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }
}
