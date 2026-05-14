package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.DateTimeUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "istDateTimeProvider")
public class JpaAuditConfig {

    @Bean
    public DateTimeProvider istDateTimeProvider() {
        return () -> Optional.of(DateTimeUtil.nowIST());
    }
}
