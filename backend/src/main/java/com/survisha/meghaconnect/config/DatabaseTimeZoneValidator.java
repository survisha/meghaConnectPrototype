package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseTimeZoneValidator {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void logTimeZoneState() {
        log.info("JVM Timezone: {}", ZoneId.systemDefault().getId());
        try {
            String databaseTimeZone = jdbcTemplate.queryForObject(
                    "SELECT CONCAT('session=', @@session.time_zone, ', global=', @@global.time_zone, ', system=', @@system_time_zone)",
                    String.class
            );
            log.info("Database Timezone: {} ({})", DateTimeUtil.IST_ZONE_ID, databaseTimeZone);
        } catch (Exception e) {
            log.warn("Database Timezone: {} (validation query failed: {})",
                    DateTimeUtil.IST_ZONE_ID, e.getMessage());
        }
    }
}
