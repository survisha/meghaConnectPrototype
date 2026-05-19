package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WalkInTokenService {

    private static final DateTimeFormatter TOKEN_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String nextToken(LocalDate tokenDate) {
        LocalDate safeDate = tokenDate != null ? tokenDate : LocalDate.now();
        jdbcTemplate.update("""
                INSERT INTO walkin_token_sequence (token_date, last_token_value)
                VALUES (?, LAST_INSERT_ID(1))
                ON DUPLICATE KEY UPDATE last_token_value = LAST_INSERT_ID(last_token_value + 1)
                """, safeDate);
        Integer next = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        return "WALKIN-" + safeDate.format(TOKEN_DATE) + "-" + String.format("%04d", next != null ? next : 1);
    }
}
